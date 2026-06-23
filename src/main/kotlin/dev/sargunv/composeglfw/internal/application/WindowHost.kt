package dev.sargunv.composeglfw.internal.application

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.RenderBackend
import dev.sargunv.composeglfw.WindowOptions
import dev.sargunv.composeglfw.WindowPlacement
import dev.sargunv.composeglfw.WindowPosition
import dev.sargunv.composeglfw.WindowState
import dev.sargunv.composeglfw.internal.input.InputDispatcher
import dev.sargunv.composeglfw.internal.platform.HostPlatformContext
import dev.sargunv.composeglfw.internal.platform.SystemThemeProvider
import dev.sargunv.composeglfw.internal.platform.currentDisplayServer
import dev.sargunv.composeglfw.internal.platform.displayName
import dev.sargunv.composeglfw.internal.render.opengl.OpenGlRenderBackend
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.scene.WindowScopeImpl
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import dev.sargunv.composeglfw.internal.window.PlatformWindowBounds
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.roundToInt
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowIconifyCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowMaximizeCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowPosCallback

internal class WindowHost(
  request: WindowRequest,
  private val uiDispatcher: UiDispatcher,
) : AutoCloseable {
  private val window =
    PlatformWindow(
      title = request.title,
      size = request.state.size,
      visible = request.visible,
      undecorated = request.undecorated,
      transparent = request.transparent,
      resizable = request.resizable,
      focusOnShow = request.focusOnShow,
      alwaysOnTop = request.alwaysOnTop,
    )
  private val renderBackend = OpenGlRenderBackend(window)
  private val platformContext = HostPlatformContext(window, request.options.textToolbar)
  private val scope = WindowScopeImpl(currentInfo(), renderBackend.interop)
  private var state = request.state
  private var options = request.options
  private var lastTitle = request.title
  private var lastVisible = request.visible
  private var lastIcon = request.icon
  private var lastUndecorated = request.undecorated
  private var lastTransparent = request.transparent
  private var lastResizable = request.resizable
  private var lastEnabled = request.enabled
  private var lastFocusOnShow = request.focusOnShow
  private var lastAlwaysOnTop = request.alwaysOnTop
  private var onPreviewKeyEvent = request.onPreviewKeyEvent
  private var onKeyEvent = request.onKeyEvent
  private var lastAppliedStateSize = request.state.size
  private var lastAppliedPosition = request.state.position
  private var lastAppliedPlacement = request.state.placement
  private var lastAppliedMinimized = request.state.isMinimized
  private var windowedBoundsBeforeFullscreen: PlatformWindowBounds? = null
  private var renderRequested = true
  private val systemThemeProvider =
    SystemThemeProvider.create { theme ->
        uiDispatcher.dispatch(
          EmptyCoroutineContext,
          {
            platformContext.updateSystemTheme(theme)
            requestRender()
          },
        )
      }
      .also { platformContext.updateSystemTheme(it.systemTheme) }
  private val scene =
    ComposeWindowScene(
      initialDensity = window.contentScale,
      // Compose layout/rendering uses framebuffer pixels to match the OpenGL/Skia target.
      initialSize = window.framebufferSize,
      platformContext = platformContext,
      coroutineContext = uiDispatcher,
      scope = scope,
      content = request.content,
      invalidate = ::requestRender,
      checkThread = { operation -> uiDispatcher.checkOwnerThread(operation) },
    )
  private val input =
    InputDispatcher(
        window = window,
        scene = scene,
        textInput = platformContext.textInput,
        onKeyboardModifiers = platformContext::updateKeyboardModifiers,
        onPreviewKeyEvent = { onPreviewKeyEvent(it) },
        onKeyEvent = { onKeyEvent(it) },
        requestRender = ::requestRender,
      )
      .also { it.enabled = request.enabled }
  private var lastFramebufferSize = window.framebufferSize
  private var lastContentScale = window.contentScale

  init {
    platformContext.updateWindowInfo()
    glfwSetFramebufferSizeCallback(window.handle) { _, _, _ ->
      window.refreshSizes()
      updateSceneMetrics()
      requestRender()
    }
    glfwSetWindowFocusCallback(window.handle) { _, focused ->
      platformContext.updateFocus(focused)
      requestRender()
    }
    glfwSetWindowCloseCallback(window.handle) { _ ->
      window.cancelCloseRequest()
      request.onCloseRequest()
    }
    glfwSetWindowPosCallback(window.handle) { _, _, _ ->
      window.refreshSizes()
      updateStateFromWindow()
    }
    glfwSetWindowIconifyCallback(window.handle) { _, iconified ->
      window.refreshSizes()
      updateStateFromWindow()
    }
    glfwSetWindowMaximizeCallback(window.handle) { _, maximized ->
      window.refreshSizes()
      updateStateFromWindow()
    }
  }

  fun update(
    title: String,
    state: WindowState,
    visible: Boolean,
    icon: Painter?,
    undecorated: Boolean,
    transparent: Boolean,
    resizable: Boolean,
    enabled: Boolean,
    focusOnShow: Boolean,
    alwaysOnTop: Boolean,
    options: WindowOptions,
  ) {
    this.state = state
    this.options = options
    if (title != lastTitle) {
      window.setTitle(title)
      lastTitle = title
    }
    if (visible != lastVisible) {
      window.setVisible(visible)
      lastVisible = visible
      requestRender()
    }
    if (icon != lastIcon) {
      // TODO: Convert Compose Painter icons into GLFW image buffers and call glfwSetWindowIcon
      // where the display server supports per-window icons.
      lastIcon = icon
    }
    if (undecorated != lastUndecorated) {
      window.setDecorated(!undecorated)
      lastUndecorated = undecorated
    }
    if (transparent != lastTransparent) {
      // TODO: Runtime transparency changes require recreating the GLFW window because
      // GLFW_TRANSPARENT_FRAMEBUFFER is a creation hint.
      lastTransparent = transparent
    }
    if (resizable != lastResizable) {
      window.setResizable(resizable)
      lastResizable = resizable
    }
    if (enabled != lastEnabled) {
      input.enabled = enabled
      lastEnabled = enabled
    }
    if (focusOnShow != lastFocusOnShow) {
      window.setFocusOnShow(focusOnShow)
      lastFocusOnShow = focusOnShow
    }
    if (alwaysOnTop != lastAlwaysOnTop) {
      window.setAlwaysOnTop(alwaysOnTop)
      lastAlwaysOnTop = alwaysOnTop
    }
    // TODO: Decide which WindowOptions are runtime-mutable. GLFW requires recreating the native
    // window for some creation-time attributes, while text toolbar content can update.
    applyStateToWindow()
  }

  fun updateAndRender() {
    applyStateToWindow()
    window.refreshSizes()
    updateSceneMetrics()
    updateStateFromWindow()
    if (!lastVisible) {
      return
    }
    if (window.framebufferSize.width <= 0 || window.framebufferSize.height <= 0) {
      return
    }
    if (!renderRequested && !scene.hasInvalidations) {
      return
    }
    renderRequested = false
    renderBackend.render(scene, System.nanoTime())
  }

  override fun close() {
    glfwSetFramebufferSizeCallback(window.handle, null)?.free()
    glfwSetWindowFocusCallback(window.handle, null)?.free()
    glfwSetWindowCloseCallback(window.handle, null)?.free()
    glfwSetWindowPosCallback(window.handle, null)?.free()
    glfwSetWindowIconifyCallback(window.handle, null)?.free()
    glfwSetWindowMaximizeCallback(window.handle, null)?.free()
    systemThemeProvider.close()
    input.close()
    platformContext.destroyLifecycle()
    scene.close()
    renderBackend.close()
    window.close()
  }

  private fun applyStateToWindow() {
    val requestedSize = state.size
    if (requestedSize != lastAppliedStateSize) {
      // TODO: Support Compose Desktop-style DpSize.Unspecified by measuring content before
      // choosing the native window size.
      window.setSize(requestedSize)
      lastAppliedStateSize = requestedSize
      requestRender()
    }

    val requestedPosition = state.position
    if (requestedPosition != lastAppliedPosition) {
      applyPosition(requestedPosition)
    }

    if (state.placement != lastAppliedPlacement) {
      applyPlacement(state.placement)
      lastAppliedPlacement = state.placement
    }

    if (state.isMinimized != lastAppliedMinimized) {
      if (state.isMinimized) {
        window.iconify()
      } else {
        window.restore()
      }
      lastAppliedMinimized = state.isMinimized
    }
  }

  private fun applyPosition(position: WindowPosition) {
    if (!window.supportsWindowPosition || window.isFullscreen) {
      // TODO: Preserve or report unresolved requested positions on display servers that restrict
      // window positioning, especially Wayland.
      lastAppliedPosition = position
      return
    }

    val absolutePosition =
      when (position) {
        is WindowPosition.Absolute -> position.toPlatformPosition()
        is WindowPosition.Aligned -> position.toPlatformPosition()
        WindowPosition.PlatformDefault -> {
          lastAppliedPosition = position
          return
        }
      }
    window.setPosition(absolutePosition)
    val resolvedPosition = absolutePosition.toWindowPosition()
    lastAppliedPosition = resolvedPosition
    if (state.position != resolvedPosition) {
      state.position = resolvedPosition
    }
    requestRender()
  }

  private fun applyPlacement(placement: WindowPlacement) {
    when (placement) {
      WindowPlacement.Floating -> {
        if (window.isFullscreen) {
          window.setWindowed(windowedBoundsBeforeFullscreen ?: window.currentWindowedBounds())
          windowedBoundsBeforeFullscreen = null
        } else {
          window.restore()
        }
      }
      WindowPlacement.Maximized -> {
        if (window.isFullscreen) {
          window.setWindowed(windowedBoundsBeforeFullscreen ?: window.currentWindowedBounds())
          windowedBoundsBeforeFullscreen = null
        }
        window.restore()
        window.maximize()
      }
      WindowPlacement.Fullscreen -> {
        if (!window.isFullscreen) {
          windowedBoundsBeforeFullscreen = window.currentWindowedBounds()
        }
        window.setFullscreen()
      }
    }
    requestRender()
  }

  private fun requestRender() {
    renderRequested = true
  }

  private fun updateSceneMetrics() {
    val framebuffer = window.framebufferSize
    if (framebuffer != lastFramebufferSize) {
      lastFramebufferSize = framebuffer
      renderBackend.resize(framebuffer)
      scene.resize(framebuffer)
      requestRender()
    }
    if (window.contentScale != lastContentScale) {
      lastContentScale = window.contentScale
      scene.updateDensity(window.contentScale)
      requestRender()
    }
    platformContext.updateWindowInfo()
    scope.updateInfo(currentInfo())
  }

  private fun updateStateFromWindow() {
    val size = DpSize(window.logicalWindowSize.width.dp, window.logicalWindowSize.height.dp)
    if (
      window.logicalWindowSize.width > 0 &&
        window.logicalWindowSize.height > 0 &&
        size != state.size
    ) {
      lastAppliedStateSize = size
      state.size = size
    }

    if (window.supportsWindowPosition && !window.isFullscreen) {
      val position = window.windowPosition.toWindowPosition()
      if (position != state.position) {
        lastAppliedPosition = position
        state.position = position
      }
    }

    val placement =
      when {
        window.isFullscreen -> WindowPlacement.Fullscreen
        window.isMaximized -> WindowPlacement.Maximized
        else -> WindowPlacement.Floating
      }
    if (placement != state.placement) {
      lastAppliedPlacement = placement
      state.placement = placement
    }

    if (window.isIconified != state.isMinimized) {
      lastAppliedMinimized = window.isIconified
      state.isMinimized = window.isIconified
    }
  }

  private fun currentInfo(): HostWindowInfo {
    val displayServer = currentDisplayServer()
    return HostWindowInfo(
      displayServer = displayServer,
      displayName = displayServer.displayName(),
      renderBackend = RenderBackend.OPENGL,
      // Framebuffer dimensions are physical drawable pixels.
      framebufferWidth = window.framebufferSize.width,
      framebufferHeight = window.framebufferSize.height,
      // Window dimensions are cross-platform logical content-area units.
      windowWidth = window.logicalWindowSize.width,
      windowHeight = window.logicalWindowSize.height,
      contentScale = window.contentScale,
    )
  }

  private fun WindowPosition.Absolute.toPlatformPosition(): IntOffset =
    IntOffset(x.value.roundToInt(), y.value.roundToInt())

  private fun WindowPosition.Aligned.toPlatformPosition(): IntOffset {
    val workArea = window.currentMonitorWorkArea()
    val offset =
      alignment.align(
        size = window.windowSize,
        space = workArea.size,
        layoutDirection = LayoutDirection.Ltr,
      )
    return workArea.position + offset
  }

  private fun IntOffset.toWindowPosition(): WindowPosition = WindowPosition(x.dp, y.dp)
}

private operator fun IntOffset.plus(offset: IntOffset): IntOffset =
  IntOffset(x + offset.x, y + offset.y)
