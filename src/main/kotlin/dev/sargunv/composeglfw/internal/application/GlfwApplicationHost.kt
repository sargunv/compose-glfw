package dev.sargunv.composeglfw.internal.application

import dev.sargunv.composeglfw.GlfwWindowSpec
import dev.sargunv.composeglfw.GlfwPlatform
import dev.sargunv.composeglfw.internal.platform.glfwPlatform
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
import org.lwjgl.glfw.GLFW.glfwGetError
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
    preferredPlatform()?.let { glfwInitHint(GLFW_PLATFORM, it.glfwPlatformHint) }
    check(glfwInit()) { "GLFW initialization failed: ${glfwGetError(null)}" }
    initialized = true
    glfwPlatform()
  }

  private fun preferredPlatform(): GlfwPlatform? {
    val requested =
      System.getProperty(PlatformProperty)
        ?: if (System.getenv("WAYLAND_DISPLAY") != null) GlfwPlatform.WAYLAND.toString() else null
    return requested?.let(GlfwPlatform::fromSelection)
  }
}

private const val PlatformProperty = "compose.glfw.platform"

private val GlfwPlatform.glfwPlatformHint: Int
  get() =
    when (this) {
      GlfwPlatform.WAYLAND -> GLFW_PLATFORM_WAYLAND
      GlfwPlatform.X11 -> GLFW_PLATFORM_X11
    }

private fun GlfwPlatform.Companion.fromSelection(value: String): GlfwPlatform =
  when (value.lowercase()) {
    "wayland" -> GlfwPlatform.WAYLAND
    "x11" -> GlfwPlatform.X11
    else -> error("Unsupported GLFW platform '$value'. Use 'wayland' or 'x11'.")
  }
