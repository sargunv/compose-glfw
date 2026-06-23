package dev.sargunv.composeglfw.internal.window

import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.GlfwWindowOptions
import dev.sargunv.composeglfw.GlfwWindowSize
import dev.sargunv.composeglfw.internal.platform.glfwPlatform
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
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_TRANSPARENT_FRAMEBUFFER
import org.lwjgl.glfw.GLFW.glfwCreateStandardCursor
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyCursor
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwFocusWindow
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetVersionString
import org.lwjgl.glfw.GLFW.glfwGetWindowContentScale
import org.lwjgl.glfw.GLFW.glfwGetWindowAttrib
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSetCursor
import org.lwjgl.glfw.GLFW.glfwSetInputMode
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL

internal class GlfwPlatformWindow(
  title: String,
  size: GlfwWindowSize,
  options: GlfwWindowOptions,
) : AutoCloseable {
  var handle: Long = NULL
    private set

  private val platform = glfwPlatform()

  // Drawable framebuffer size in physical pixels. This is the Skia target and ComposeScene size.
  var framebufferSize: IntSize = IntSize(size.width, size.height)
    private set

  // GLFW content-area size in screen coordinates. Cursor positions use this same coordinate space.
  var windowSize: IntSize = IntSize(size.width, size.height)
    private set

  // GLFW window position in screen coordinates. Wayland does not expose this, so it remains zero there.
  var windowPosition: IntOffset = IntOffset.Zero
    private set

  val supportsWindowPosition: Boolean = platform.supportsWindowPosition

  // GLFW content scale, used as the Compose density for px-to-dp conversion.
  var contentScale: Float = 1f
    private set

  val shouldClose: Boolean
    get() = glfwWindowShouldClose(handle)

  val isFocused: Boolean
    get() = glfwGetWindowAttrib(handle, GLFW_FOCUSED) == GLFW_TRUE

  val isTransparent: Boolean = options.transparentFramebuffer

  private val standardCursors = mutableMapOf<Int, Long>()

  init {
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
    glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_RESIZABLE, if (options.resizable) GLFW_TRUE else GLFW_FALSE)
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

    println("GLFW ${glfwGetVersionString()}")
    println("GLFW platform: $platform")
    println("Wayland display: ${System.getenv("WAYLAND_DISPLAY") ?: "<unset>"}")
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
    val shape = pointerIcon.glfwCursorShape()
    val cursor =
      if (shape == null) {
        NULL
      } else {
        standardCursors[shape] ?: glfwCreateStandardCursor(shape).also { cursor ->
          if (cursor != NULL) {
            standardCursors[shape] = cursor
          }
        }
      }
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

    // No currently supported platform exposes this. Add the glfwGetWindowPos call when one does.
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
      glfwDestroyWindow(handle)
      handle = NULL
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
