package dev.sargunv.composeglfw.internal.window

import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.CursorImagePointerIcon
import dev.sargunv.composeglfw.DisplayServer
import dev.sargunv.composeglfw.internal.platform.currentDisplayServer
import dev.sargunv.composeglfw.internal.platform.macos.MacObjectiveC
import dev.sargunv.composeglfw.internal.platform.windows.configureDirectCompositionHost
import kotlin.math.roundToInt
import org.lwjgl.glfw.GLFW.GLFW_ARROW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_CROSSHAIR_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_DECORATED
import org.lwjgl.glfw.GLFW.GLFW_DONT_CARE
import org.lwjgl.glfw.GLFW.GLFW_EGL_CONTEXT_API
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_FLOATING
import org.lwjgl.glfw.GLFW.GLFW_FOCUSED
import org.lwjgl.glfw.GLFW.GLFW_FOCUS_ON_SHOW
import org.lwjgl.glfw.GLFW.GLFW_IBEAM_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_ICONIFIED
import org.lwjgl.glfw.GLFW.GLFW_LOCK_KEY_MODS
import org.lwjgl.glfw.GLFW.GLFW_MAXIMIZED
import org.lwjgl.glfw.GLFW.GLFW_NO_API
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_POINTING_HAND_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_SCALE_TO_MONITOR
import org.lwjgl.glfw.GLFW.GLFW_TRANSPARENT_FRAMEBUFFER
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.GLFW_X11_ONTHESPOT
import org.lwjgl.glfw.GLFW.glfwCreateCursor
import org.lwjgl.glfw.GLFW.glfwCreateStandardCursor
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyCursor
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwFocusWindow
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetMonitorWorkarea
import org.lwjgl.glfw.GLFW.glfwGetMonitors
import org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor
import org.lwjgl.glfw.GLFW.glfwGetVideoMode
import org.lwjgl.glfw.GLFW.glfwGetWindowAttrib
import org.lwjgl.glfw.GLFW.glfwGetWindowContentScale
import org.lwjgl.glfw.GLFW.glfwGetWindowMonitor
import org.lwjgl.glfw.GLFW.glfwGetWindowPos
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwHideWindow
import org.lwjgl.glfw.GLFW.glfwIconifyWindow
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwMaximizeWindow
import org.lwjgl.glfw.GLFW.glfwRestoreWindow
import org.lwjgl.glfw.GLFW.glfwSetCursor
import org.lwjgl.glfw.GLFW.glfwSetInputMode
import org.lwjgl.glfw.GLFW.glfwSetWindowAttrib
import org.lwjgl.glfw.GLFW.glfwSetWindowMonitor
import org.lwjgl.glfw.GLFW.glfwSetWindowPos
import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.glfw.GLFW.glfwSetWindowSize
import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL

internal class PlatformWindow(
  title: String,
  size: DpSize,
  visible: Boolean,
  undecorated: Boolean,
  transparent: Boolean,
  resizable: Boolean,
  focusOnShow: Boolean,
  alwaysOnTop: Boolean,
  private val clientApi: WindowClientApi,
) : AutoCloseable {
  private val initialWindowSize = size.toGlfwWindowSize()

  var handle: Long = NULL
    private set

  private val displayServer = currentDisplayServer()

  // Drawable framebuffer size in physical pixels. This is the Skia target and ComposeScene size.
  var framebufferSize: IntSize = initialWindowSize
    private set

  // GLFW content-area size in screen coordinates. Cursor positions use this same coordinate space.
  // On Wayland this is usually logical pixels; on X11 this is usually physical pixels.
  var windowSize: IntSize = initialWindowSize
    private set

  // Cross-platform logical content-area size. Wayland already reports this in window units; X11
  // and Win32 need conversion from physical framebuffer pixels on scaled desktops.
  val logicalWindowSize: IntSize
    get() =
      when (displayServer) {
        DisplayServer.WAYLAND,
        DisplayServer.COCOA -> windowSize
        DisplayServer.WIN32,
        DisplayServer.X11 ->
          IntSize(
            (framebufferSize.width / contentScale).roundToInt().coerceAtLeast(0),
            (framebufferSize.height / contentScale).roundToInt().coerceAtLeast(0),
          )
      }

  // GLFW window position in screen coordinates. Platforms that do not expose it keep this at zero.
  var windowPosition: IntOffset = IntOffset.Zero
    private set

  val supportsWindowPosition: Boolean = displayServer.supportsWindowPosition

  val reportsPreEventKeyModifiers: Boolean = displayServer.reportsPreEventKeyModifiers

  // GLFW content scale, used as the Compose density for px-to-dp conversion.
  var contentScale: Float = 1f
    private set

  val shouldClose: Boolean
    get() = glfwWindowShouldClose(handle)

  val isFocused: Boolean
    get() = glfwGetWindowAttrib(handle, GLFW_FOCUSED) == GLFW_TRUE

  val isIconified: Boolean
    get() = glfwGetWindowAttrib(handle, GLFW_ICONIFIED) == GLFW_TRUE

  val isMaximized: Boolean
    get() = glfwGetWindowAttrib(handle, GLFW_MAXIMIZED) == GLFW_TRUE

  val isFullscreen: Boolean
    get() =
      when (displayServer) {
        DisplayServer.COCOA -> macNativeFullscreenTarget ?: isMacNativeFullscreen()
        DisplayServer.WAYLAND,
        DisplayServer.X11,
        DisplayServer.WIN32 -> glfwGetWindowMonitor(handle) != NULL
      }

  val isTransparent: Boolean = transparent

  private val standardCursors = mutableMapOf<Int, Long>()
  private val imageCursors = mutableMapOf<ImageCursorKey, Long>()
  private var glCapabilities: GLCapabilities? = null
  private var macNativeFullscreenTarget: Boolean? = null

  init {
    glfwDefaultWindowHints()
    when (clientApi) {
      WindowClientApi.OPENGL_EGL -> {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
      }
      WindowClientApi.NO_API -> {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
      }
    }
    glfwWindowHint(GLFW_DECORATED, if (undecorated) GLFW_FALSE else GLFW_TRUE)
    glfwWindowHint(GLFW_RESIZABLE, if (resizable) GLFW_TRUE else GLFW_FALSE)
    glfwWindowHint(GLFW_FOCUSED, if (focusOnShow) GLFW_TRUE else GLFW_FALSE)
    glfwWindowHint(GLFW_FOCUS_ON_SHOW, if (focusOnShow) GLFW_TRUE else GLFW_FALSE)
    glfwWindowHint(GLFW_FLOATING, if (alwaysOnTop) GLFW_TRUE else GLFW_FALSE)
    // On X11, screen coordinates and pixels are 1:1, so GLFW needs this to create windows at
    // the requested logical size on scaled desktops. Wayland uses framebuffer scaling instead.
    glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE)
    if (displayServer == DisplayServer.X11) {
      glfwWindowHint(GLFW_X11_ONTHESPOT, GLFW_TRUE)
    }
    glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, if (transparent) GLFW_TRUE else GLFW_FALSE)
    glfwWindowHint(GLFW_VISIBLE, if (visible) GLFW_TRUE else GLFW_FALSE)

    handle = glfwCreateWindow(initialWindowSize.width, initialWindowSize.height, title, NULL, NULL)
    check(handle != NULL) { "GLFW window creation failed: ${glfwGetError(null)}" }
    setDecorated(!undecorated)
    if (displayServer == DisplayServer.COCOA) {
      configureMacNativeFullscreen()
    }
    if (
      displayServer == DisplayServer.WIN32 && clientApi == WindowClientApi.NO_API && transparent
    ) {
      configureDirectCompositionHost(glfwGetWin32Window(handle))
    }
    glfwSetInputMode(handle, GLFW_LOCK_KEY_MODS, GLFW_TRUE)
    if (clientApi == WindowClientApi.OPENGL_EGL) {
      makeCurrent()
      glfwSwapInterval(1)
      glCapabilities = GL.createCapabilities()
    }
    refreshWindowPosition()
    readWindowSize()
    readFramebufferSize()
    readContentScale()
  }

  fun makeCurrent() {
    check(clientApi == WindowClientApi.OPENGL_EGL) {
      "makeCurrent is only available for OpenGL windows"
    }
    glfwMakeContextCurrent(handle)
    glCapabilities?.let(GL::setCapabilities)
  }

  fun swapBuffers() {
    check(clientApi == WindowClientApi.OPENGL_EGL) {
      "swapBuffers is only available for OpenGL windows"
    }
    glfwSwapBuffers(handle)
  }

  fun requestFocus() {
    glfwFocusWindow(handle)
  }

  fun setTitle(title: String) {
    glfwSetWindowTitle(handle, title)
  }

  fun setVisible(visible: Boolean) {
    if (visible) {
      glfwShowWindow(handle)
    } else {
      glfwHideWindow(handle)
    }
  }

  fun setDecorated(decorated: Boolean) {
    glfwSetWindowAttrib(handle, GLFW_DECORATED, if (decorated) GLFW_TRUE else GLFW_FALSE)
  }

  fun setResizable(resizable: Boolean) {
    glfwSetWindowAttrib(handle, GLFW_RESIZABLE, if (resizable) GLFW_TRUE else GLFW_FALSE)
  }

  fun setAlwaysOnTop(alwaysOnTop: Boolean) {
    glfwSetWindowAttrib(handle, GLFW_FLOATING, if (alwaysOnTop) GLFW_TRUE else GLFW_FALSE)
  }

  fun setFocusOnShow(focusOnShow: Boolean) {
    glfwSetWindowAttrib(handle, GLFW_FOCUS_ON_SHOW, if (focusOnShow) GLFW_TRUE else GLFW_FALSE)
  }

  fun setSize(size: DpSize) {
    val windowSize = size.toRuntimeGlfwWindowSize()
    glfwSetWindowSize(handle, windowSize.width, windowSize.height)
    refreshSizes()
  }

  fun setPosition(position: IntOffset) {
    if (supportsWindowPosition) {
      glfwSetWindowPos(handle, position.x, position.y)
      refreshWindowPosition()
    }
  }

  fun setFullscreen() {
    if (displayServer == DisplayServer.COCOA) {
      setMacNativeFullscreen(true)
      refreshSizes()
      return
    }

    val monitor = currentMonitor()
    val videoMode = glfwGetVideoMode(monitor) ?: error("GLFW monitor has no video mode")
    glfwSetWindowMonitor(
      handle,
      monitor,
      0,
      0,
      videoMode.width(),
      videoMode.height(),
      videoMode.refreshRate(),
    )
    refreshSizes()
  }

  fun setWindowed(bounds: PlatformWindowBounds) {
    if (displayServer == DisplayServer.COCOA && isFullscreen) {
      setMacNativeFullscreen(false)
      refreshSizes()
      return
    }

    glfwSetWindowMonitor(
      handle,
      NULL,
      bounds.position.x,
      bounds.position.y,
      bounds.size.width,
      bounds.size.height,
      GLFW_DONT_CARE,
    )
    refreshSizes()
  }

  fun currentWindowedBounds(): PlatformWindowBounds =
    PlatformWindowBounds(
      position = windowPosition,
      size = windowSize,
    )

  fun currentMonitorWorkArea(): PlatformRect = monitorWorkArea(currentMonitor())

  fun maximize() {
    glfwMaximizeWindow(handle)
    refreshSizes()
  }

  fun restore() {
    glfwRestoreWindow(handle)
    refreshSizes()
  }

  fun iconify() {
    glfwIconifyWindow(handle)
  }

  fun cancelCloseRequest() {
    glfwSetWindowShouldClose(handle, false)
  }

  fun setPointerIcon(pointerIcon: PointerIcon) {
    val cursor = pointerIcon.glfwCursor()
    glfwSetCursor(handle, cursor)
  }

  fun refreshSizes() {
    refreshWindowPosition()
    readWindowSize()
    readFramebufferSize()
    readContentScale()
    refreshMacNativeFullscreenTarget()
  }

  private fun refreshWindowPosition() {
    if (!supportsWindowPosition) {
      windowPosition = IntOffset.Zero
      return
    }

    MemoryStack.stackPush().use { stack ->
      val x = stack.mallocInt(1)
      val y = stack.mallocInt(1)
      glfwGetWindowPos(handle, x, y)
      windowPosition = IntOffset(x[0], y[0])
    }
  }

  fun readFramebufferSize() {
    MemoryStack.stackPush().use { stack ->
      val width = stack.mallocInt(1)
      val height = stack.mallocInt(1)
      glfwGetFramebufferSize(handle, width, height)
      framebufferSize = IntSize(width[0].coerceAtLeast(0), height[0].coerceAtLeast(0))
    }
  }

  private fun readWindowSize() {
    MemoryStack.stackPush().use { stack ->
      val width = stack.mallocInt(1)
      val height = stack.mallocInt(1)
      glfwGetWindowSize(handle, width, height)
      windowSize = IntSize(width[0].coerceAtLeast(1), height[0].coerceAtLeast(1))
    }
  }

  private fun readContentScale() {
    MemoryStack.stackPush().use { stack ->
      val x = stack.mallocFloat(1)
      val y = stack.mallocFloat(1)
      glfwGetWindowContentScale(handle, x, y)
      contentScale = maxOf(x[0], y[0], 1f)
    }
  }

  private fun currentMonitor(): Long {
    val fullscreenMonitor = glfwGetWindowMonitor(handle)
    if (fullscreenMonitor != NULL) {
      return fullscreenMonitor
    }

    val monitors = glfwGetMonitors()
    if (monitors != null) {
      val center =
        IntOffset(
          x = windowPosition.x + windowSize.width / 2,
          y = windowPosition.y + windowSize.height / 2,
        )
      for (index in 0 until monitors.limit()) {
        val monitor = monitors[index]
        if (monitorWorkArea(monitor).contains(center)) {
          return monitor
        }
      }
    }
    val primary = glfwGetPrimaryMonitor()
    check(primary != NULL) { "GLFW did not report a primary monitor" }
    return primary
  }

  private fun monitorWorkArea(monitor: Long): PlatformRect =
    MemoryStack.stackPush().use { stack ->
      val x = stack.mallocInt(1)
      val y = stack.mallocInt(1)
      val width = stack.mallocInt(1)
      val height = stack.mallocInt(1)
      glfwGetMonitorWorkarea(monitor, x, y, width, height)
      PlatformRect(
        position = IntOffset(x[0], y[0]),
        size = IntSize(width[0].coerceAtLeast(1), height[0].coerceAtLeast(1)),
      )
    }

  override fun close() {
    if (handle != NULL) {
      standardCursors.values.forEach(::glfwDestroyCursor)
      standardCursors.clear()
      imageCursors.values.forEach(::glfwDestroyCursor)
      imageCursors.clear()
      glfwDestroyWindow(handle)
      handle = NULL
    }
  }

  private fun PointerIcon.glfwCursor(): Long =
    when (this) {
      is CursorImagePointerIcon -> {
        val key = imageCursorKey(displayServer, contentScale)
        imageCursors[key] ?: createImageCursor(key)
      }
      else -> standardCursor()
    }

  private fun PointerIcon.standardCursor(): Long {
    val shape = glfwCursorShape() ?: return NULL
    return standardCursors[shape]
      ?: glfwCreateStandardCursor(shape).also { cursor ->
        if (cursor != NULL) {
          standardCursors[shape] = cursor
        }
      }
  }

  private fun createImageCursor(key: ImageCursorKey): Long {
    key.toNativeCursorImage().use { image ->
      val cursor =
        MemoryStack.stackPush().use { stack ->
          val glfwImage =
            GLFWImage.malloc(stack).width(image.width).height(image.height).pixels(image.rgbaPixels)
          glfwCreateCursor(glfwImage, image.xhot, image.yhot)
        }
      if (cursor != NULL) {
        imageCursors[key] = cursor
      }
      return cursor
    }
  }

  private fun configureMacNativeFullscreen() {
    val nsWindow = glfwGetCocoaWindow(handle)
    if (nsWindow == NULL) {
      return
    }

    val collectionBehavior = MacObjectiveC.sendPointer(nsWindow, "collectionBehavior")
    MacObjectiveC.sendVoid(
      nsWindow,
      "setCollectionBehavior:",
      collectionBehavior or NsWindowCollectionBehaviorFullScreenPrimary,
    )
  }

  private fun setMacNativeFullscreen(fullscreen: Boolean) {
    val current = isMacNativeFullscreen()
    if (current == fullscreen) {
      macNativeFullscreenTarget = null
      return
    }
    if (macNativeFullscreenTarget != fullscreen) {
      macNativeFullscreenTarget = fullscreen
      MacObjectiveC.sendVoid(glfwGetCocoaWindow(handle), "toggleFullScreen:", NULL)
    }
  }

  private fun isMacNativeFullscreen(): Boolean {
    val nsWindow = glfwGetCocoaWindow(handle)
    if (nsWindow == NULL) {
      return false
    }
    return MacObjectiveC.sendPointer(nsWindow, "styleMask") and NsWindowStyleMaskFullScreen != 0L
  }

  private fun refreshMacNativeFullscreenTarget() {
    val target = macNativeFullscreenTarget ?: return
    if (displayServer == DisplayServer.COCOA && isMacNativeFullscreen() == target) {
      macNativeFullscreenTarget = null
    }
  }

  private fun DpSize.toRuntimeGlfwWindowSize(): IntSize {
    val scale =
      when (displayServer) {
        DisplayServer.X11,
        DisplayServer.WIN32 -> contentScale
        DisplayServer.WAYLAND,
        DisplayServer.COCOA -> 1f
      }
    return IntSize(
      width = width.toGlfwWindowUnit("width", scale),
      height = height.toGlfwWindowUnit("height", scale),
    )
  }
}

private const val NsWindowCollectionBehaviorFullScreenPrimary = 1L shl 7
private const val NsWindowStyleMaskFullScreen = 1L shl 14

internal enum class WindowClientApi {
  OPENGL_EGL,
  NO_API,
}

internal data class PlatformWindowBounds(
  val position: IntOffset,
  val size: IntSize,
)

internal data class PlatformRect(
  val position: IntOffset,
  val size: IntSize,
) {
  val right: Int
    get() = position.x + size.width

  val bottom: Int
    get() = position.y + size.height

  fun contains(point: IntOffset): Boolean =
    point.x >= position.x && point.x < right && point.y >= position.y && point.y < bottom
}

private fun PointerIcon.glfwCursorShape(): Int? =
  when (this) {
    PointerIcon.Default -> GLFW_ARROW_CURSOR
    PointerIcon.Crosshair -> GLFW_CROSSHAIR_CURSOR
    PointerIcon.Text -> GLFW_IBEAM_CURSOR
    PointerIcon.Hand -> GLFW_POINTING_HAND_CURSOR
    else -> null
  }

private fun DpSize.toGlfwWindowSize(): IntSize =
  IntSize(
    width = width.toGlfwWindowUnit("width"),
    height = height.toGlfwWindowUnit("height"),
  )

private fun Dp.toGlfwWindowUnit(name: String, scale: Float = 1f): Int {
  require(java.lang.Float.isFinite(value) && value > 0f) {
    "Window $name must be a positive finite Dp value"
  }
  return (value * scale).roundToInt().coerceAtLeast(1)
}
