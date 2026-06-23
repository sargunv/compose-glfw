package dev.sargunv.composeglfw.internal.window

import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.GlfwWindowOptions
import dev.sargunv.composeglfw.GlfwWindowSize
import dev.sargunv.composeglfw.internal.platform.glfwPlatformName
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_EGL_CONTEXT_API
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetVersionString
import org.lwjgl.glfw.GLFW.glfwGetWindowContentScale
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL

internal class GlfwPlatformWindow(
  private val title: String,
  size: GlfwWindowSize,
  options: GlfwWindowOptions,
) : AutoCloseable {
  var handle: Long = NULL
    private set

  var framebufferSize: IntSize = IntSize(size.width, size.height)
    private set

  var windowSize: IntSize = IntSize(size.width, size.height)
    private set

  var contentScale: Float = 1f
    private set

  val shouldClose: Boolean
    get() = glfwWindowShouldClose(handle)

  init {
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
    glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_RESIZABLE, if (options.resizable) GLFW_TRUE else GLFW_FALSE)

    handle = glfwCreateWindow(size.width, size.height, title, NULL, NULL)
    check(handle != NULL) { "GLFW window creation failed: ${glfwGetError(null)}" }
    makeCurrent()
    glfwSwapInterval(1)
    GL.createCapabilities()
    readWindowSize()
    readFramebufferSize()
    readContentScale()

    println("GLFW ${glfwGetVersionString()}")
    println("GLFW platform: ${glfwPlatformName()}")
    println("Wayland display: ${System.getenv("WAYLAND_DISPLAY") ?: "<unset>"}")
  }

  fun makeCurrent() {
    glfwMakeContextCurrent(handle)
  }

  fun swapBuffers() {
    glfwSwapBuffers(handle)
  }

  fun refreshSizes() {
    readWindowSize()
    readFramebufferSize()
    readContentScale()
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
      glfwDestroyWindow(handle)
      handle = NULL
    }
  }
}
