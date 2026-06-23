package dev.sargunv.composeglfw.internal.window

import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.CursorImagePointerIcon
import dev.sargunv.composeglfw.WindowOptions
import dev.sargunv.composeglfw.WindowSize
import dev.sargunv.composeglfw.internal.platform.currentDisplayServer
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_EGL_CONTEXT_API
import org.lwjgl.glfw.GLFW.GLFW_ARROW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CROSSHAIR_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_FOCUSED
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_IBEAM_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_LOCK_KEY_MODS
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_POINTING_HAND_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_SCALE_TO_MONITOR
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_TRANSPARENT_FRAMEBUFFER
import org.lwjgl.glfw.GLFW.glfwCreateCursor
import org.lwjgl.glfw.GLFW.glfwCreateStandardCursor
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyCursor
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwFocusWindow
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetWindowContentScale
import org.lwjgl.glfw.GLFW.glfwGetWindowAttrib
import org.lwjgl.glfw.GLFW.glfwGetWindowPos
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSetCursor
import org.lwjgl.glfw.GLFW.glfwSetInputMode
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import kotlin.math.roundToInt

internal class PlatformWindow(
  title: String,
  size: WindowSize,
  options: WindowOptions,
) : AutoCloseable {
  var handle: Long = NULL
    private set

  private val displayServer = currentDisplayServer()

  // Drawable framebuffer size in physical pixels. This is the Skia target and ComposeScene size.
  var framebufferSize: IntSize = IntSize(size.width, size.height)
    private set

  // GLFW content-area size in screen coordinates. Cursor positions use this same coordinate space.
  // On Wayland this is usually logical pixels; on X11 this is usually physical pixels.
  var windowSize: IntSize = IntSize(size.width, size.height)
    private set

  // Cross-platform logical content-area size, matching the Compose density applied to the
  // framebuffer-backed scene.
  val logicalWindowSize: IntSize
    get() =
      IntSize(
        (framebufferSize.width / contentScale).roundToInt().coerceAtLeast(0),
        (framebufferSize.height / contentScale).roundToInt().coerceAtLeast(0),
      )

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

  val isTransparent: Boolean = options.transparentFramebuffer

  private val standardCursors = mutableMapOf<Int, Long>()
  private val imageCursors = mutableMapOf<CursorImagePointerIcon, Long>()

  init {
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
    glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_RESIZABLE, if (options.resizable) GLFW_TRUE else GLFW_FALSE)
    // On X11, screen coordinates and pixels are 1:1, so GLFW needs this to create windows at
    // the requested logical size on scaled desktops. Wayland uses framebuffer scaling instead.
    glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE)
    glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, if (options.transparentFramebuffer) GLFW_TRUE else GLFW_FALSE)

    handle = glfwCreateWindow(size.width, size.height, title, NULL, NULL)
    check(handle != NULL) { "GLFW window creation failed: ${glfwGetError(null)}" }
    glfwSetInputMode(handle, GLFW_LOCK_KEY_MODS, GLFW_TRUE)
    makeCurrent()
    glfwSwapInterval(1)
    GL.createCapabilities()
    refreshWindowPosition()
    readWindowSize()
    readFramebufferSize()
    readContentScale()
  }

  fun makeCurrent() {
    glfwMakeContextCurrent(handle)
  }

  fun swapBuffers() {
    glfwSwapBuffers(handle)
  }

  fun requestFocus() {
    glfwFocusWindow(handle)
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
      is CursorImagePointerIcon -> imageCursors[this] ?: createImageCursor(this)
      else -> standardCursor()
    }

  private fun PointerIcon.standardCursor(): Long {
    val shape = glfwCursorShape() ?: return NULL
    return standardCursors[shape] ?: glfwCreateStandardCursor(shape).also { cursor ->
      if (cursor != NULL) {
        standardCursors[shape] = cursor
      }
    }
  }

  private fun createImageCursor(pointerIcon: CursorImagePointerIcon): Long {
    val image = pointerIcon.image
    val argbPixels = IntArray(image.width * image.height)
    image.readPixels(argbPixels)

    val rgbaPixels = memAlloc(argbPixels.size * 4)
    try {
      argbPixels.forEachIndexed { index, argb ->
        val offset = index * 4
        rgbaPixels.put(offset, ((argb shr 16) and 0xff).toByte())
        rgbaPixels.put(offset + 1, ((argb shr 8) and 0xff).toByte())
        rgbaPixels.put(offset + 2, (argb and 0xff).toByte())
        rgbaPixels.put(offset + 3, ((argb ushr 24) and 0xff).toByte())
      }

      val cursor =
        MemoryStack.stackPush().use { stack ->
          val glfwImage =
            GLFWImage
              .malloc(stack)
              .width(image.width)
              .height(image.height)
              .pixels(rgbaPixels)
          glfwCreateCursor(glfwImage, pointerIcon.hotSpot.x, pointerIcon.hotSpot.y)
        }
      if (cursor != NULL) {
        imageCursors[pointerIcon] = cursor
      }
      return cursor
    } finally {
      memFree(rgbaPixels)
    }
  }
}

private fun PointerIcon.glfwCursorShape(): Int? =
  when (this) {
    PointerIcon.Default -> GLFW_ARROW_CURSOR
    PointerIcon.Crosshair -> GLFW_CROSSHAIR_CURSOR
    PointerIcon.Text -> GLFW_IBEAM_CURSOR
    PointerIcon.Hand -> GLFW_POINTING_HAND_CURSOR
    else -> null
  }
