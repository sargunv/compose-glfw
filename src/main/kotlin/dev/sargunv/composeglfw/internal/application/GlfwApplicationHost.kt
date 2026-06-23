package dev.sargunv.composeglfw.internal.application

import dev.sargunv.composeglfw.GlfwWindowSpec
import dev.sargunv.composeglfw.internal.platform.glfwPlatformName
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFW.glfwGetPlatform
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwInitHint
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetErrorCallback
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.system.MemoryUtil.memUTF8

internal class GlfwApplicationHost(private val windowSpecs: List<GlfwWindowSpec>) : AutoCloseable {
  private val windows = mutableListOf<GlfwWindowHost>()
  private val uiDispatcher = GlfwUiDispatcher()
  private var initialized = false

  fun run() {
    require(windowSpecs.isNotEmpty()) { "glfwApplication must declare at least one Window" }
    uiDispatcher.bindToCurrentThread()
    initializeGlfw()
    windowSpecs.mapTo(windows) { GlfwWindowHost(it, uiDispatcher) }

    while (windows.any { !it.shouldClose }) {
      uiDispatcher.drain()
      glfwPollEvents()
      uiDispatcher.drain()
      windows.forEach { window ->
        if (!window.shouldClose) {
          window.updateAndRender()
        }
      }
      uiDispatcher.drain()
    }
  }

  override fun close() {
    windows.asReversed().forEach { it.close() }
    windows.clear()
    if (initialized) {
      glfwTerminate()
      initialized = false
    }
    glfwSetErrorCallback(null)?.free()
  }

  private fun initializeGlfw() {
    glfwSetErrorCallback { code, description ->
      System.err.println("GLFW error $code: ${memUTF8(description)}")
    }
    if (System.getenv("WAYLAND_DISPLAY") != null) {
      glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND)
    }
    check(glfwInit()) { "GLFW initialization failed: ${glfwGetError(null)}" }
    initialized = true
    check(glfwGetPlatform() == GLFW_PLATFORM_WAYLAND) {
      "Only GLFW Wayland is supported; GLFW selected ${glfwPlatformName()}"
    }
  }
}
