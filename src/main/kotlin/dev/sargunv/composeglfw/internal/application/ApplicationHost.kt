@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.application

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import dev.sargunv.composeglfw.ApplicationScope
import dev.sargunv.composeglfw.DisplayServer
import dev.sargunv.composeglfw.HostWindowScope
import dev.sargunv.composeglfw.WindowOptions
import dev.sargunv.composeglfw.WindowState
import dev.sargunv.composeglfw.internal.platform.HostOperatingSystem
import dev.sargunv.composeglfw.internal.platform.currentDisplayServer
import dev.sargunv.composeglfw.internal.platform.hostOperatingSystem
import java.util.concurrent.locks.LockSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwInitHint
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwPostEmptyEvent
import org.lwjgl.glfw.GLFW.glfwSetErrorCallback
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWaitEvents
import org.lwjgl.system.MemoryUtil.memUTF8

internal data class WindowRequest(
  val title: String,
  val state: WindowState,
  val visible: Boolean,
  val undecorated: Boolean,
  val transparent: Boolean,
  val resizable: Boolean,
  val enabled: Boolean,
  val focusOnShow: Boolean,
  val alwaysOnTop: Boolean,
  val options: WindowOptions,
  val onCloseRequest: () -> Unit,
  val onPreviewKeyEvent: (KeyEvent) -> Boolean,
  val onKeyEvent: (KeyEvent) -> Boolean,
  val content: @Composable HostWindowScope.() -> Unit,
)

internal class ApplicationHost(private val content: @Composable ApplicationScope.() -> Unit) :
  AutoCloseable {
  private val windows = mutableListOf<WindowHost>()
  private val uiDispatcher = UiDispatcher(::wakeEventLoop)
  private val applicationScope = ApplicationScopeImpl(::createWindow, ::disposeWindow)
  private var initialized = false
  private var coroutineScope: CoroutineScope? = null
  private var recomposer: Recomposer? = null
  private var recomposerJob: Job? = null
  private var composition: Composition? = null
  private var displayServer: DisplayServer? = null

  fun run() {
    uiDispatcher.bindToCurrentThread()
    initializeGlfw()

    val context = uiDispatcher + YieldFrameClock
    coroutineScope = CoroutineScope(context)
    recomposer = Recomposer(context)
    recomposerJob = coroutineScope?.launch { recomposer?.runRecomposeAndApplyChanges() }
    composition = Composition(ApplicationApplier, checkNotNull(recomposer))
    composition?.setContent {
      if (applicationScope.isOpen) {
        applicationScope.content()
      }
    }

    runEventLoop()
  }

  override fun close() {
    composition?.dispose()
    composition = null
    recomposer?.close()
    recomposer = null
    recomposerJob?.cancel()
    recomposerJob = null
    coroutineScope?.cancel()
    coroutineScope = null
    windows.asReversed().forEach { it.close() }
    windows.clear()
    displayServer = null
    if (initialized) {
      glfwTerminate()
      initialized = false
    }
    glfwSetErrorCallback(null)?.free()
  }

  private fun runEventLoop() {
    var hasCompletedFirstFrame = false
    while (applicationScope.isOpen && (!hasCompletedFirstFrame || windows.isNotEmpty())) {
      uiDispatcher.drain()
      if (!applicationScope.isOpen) {
        break
      }
      if (hasCompletedFirstFrame && !hasPendingWork()) {
        waitForEvents()
      } else {
        glfwPollEvents()
      }
      uiDispatcher.drain()
      windows.toList().forEach { window ->
        if (window in windows) {
          window.updateAndRender()
        }
      }
      uiDispatcher.drain()
      hasCompletedFirstFrame = true
    }
  }

  private fun createWindow(request: WindowRequest): WindowHost =
    WindowHost(request, uiDispatcher, ::wakeEventLoop).also { windows += it }

  private fun disposeWindow(window: WindowHost) {
    if (windows.remove(window)) {
      window.close()
    }
  }

  private fun initializeGlfw() {
    glfwSetErrorCallback { code, description ->
      System.err.println("GLFW error $code: ${memUTF8(description)}")
    }
    preferredPlatform()?.let { glfwInitHint(GLFW_PLATFORM, it.glfwPlatformHint) }
    check(glfwInit()) { "GLFW initialization failed: ${glfwGetError(null)}" }
    initialized = true
    displayServer = currentDisplayServer()
  }

  private fun hasPendingWork(): Boolean = uiDispatcher.hasTasks || windows.any { it.hasPendingWork }

  private fun waitForEvents() {
    glfwWaitEvents()

    // GLFW's Wayland backend can wake for internal EGL/Wayland events after a buffer swap without
    // delivering app callbacks or Compose invalidations. GLFW does not expose an app-visible event
    // counter, so only the Wayland idle path backs off, and only when the wake produced no work.
    if (displayServer == DisplayServer.WAYLAND && !hasPendingWork()) {
      LockSupport.parkNanos(WaylandIdleWakeBackoffNanos)
    }
  }

  private fun wakeEventLoop() {
    if (initialized && !uiDispatcher.isOwnerThread()) {
      uiDispatcher.unparkOwnerThread()
      glfwPostEmptyEvent()
    }
  }

  private fun preferredPlatform(): DisplayServer? {
    val requested =
      System.getProperty(PlatformProperty)
        ?: if (
          hostOperatingSystem == HostOperatingSystem.LINUX &&
            System.getenv("WAYLAND_DISPLAY") != null
        ) {
          DisplayServer.WAYLAND.toString()
        } else {
          null
        }
    return requested?.let(DisplayServer::fromSelection)
  }
}

internal class ApplicationScopeImpl(
  private val createWindow: (WindowRequest) -> WindowHost,
  private val disposeWindow: (WindowHost) -> Unit,
) : ApplicationScope {
  var isOpen: Boolean by mutableStateOf(true)
    private set

  override fun exitApplication() {
    isOpen = false
  }

  fun createWindow(request: WindowRequest): WindowHost = createWindow.invoke(request)

  fun disposeWindow(window: WindowHost) {
    disposeWindow.invoke(window)
  }
}

private object ApplicationApplier : Applier<Any> {
  override val current: Any = Unit

  override fun down(node: Any) = Unit

  override fun up() = Unit

  override fun insertTopDown(
    index: Int,
    instance: Any,
  ) {
    rejectNode(instance)
  }

  override fun insertBottomUp(
    index: Int,
    instance: Any,
  ) {
    rejectNode(instance)
  }

  override fun remove(
    index: Int,
    count: Int,
  ) = Unit

  override fun move(
    from: Int,
    to: Int,
    count: Int,
  ) = Unit

  override fun clear() = Unit

  override fun onEndChanges() = Unit

  private fun rejectNode(instance: Any) {
    if (instance != Unit) {
      error("Composable content may not be emitted directly into glfwApplication")
    }
  }
}

private object YieldFrameClock : MonotonicFrameClock {
  override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
    yield()
    return onFrame(System.nanoTime())
  }
}

private const val PlatformProperty = "compose.glfw.platform"
private const val WaylandIdleWakeBackoffNanos = 4_000_000L
