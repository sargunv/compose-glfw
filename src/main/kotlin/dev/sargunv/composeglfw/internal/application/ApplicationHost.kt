package dev.sargunv.composeglfw.internal.application

import dev.sargunv.composeglfw.DisplayServer
import dev.sargunv.composeglfw.WindowSpec
import dev.sargunv.composeglfw.internal.platform.currentDisplayServer
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

internal class ApplicationHost(private val windowSpecs: List<WindowSpec>) : AutoCloseable {
  private val windows = mutableListOf<WindowHost>()
  private val uiDispatcher = UiDispatcher()
  private var initialized = false

  fun run() {
    require(windowSpecs.isNotEmpty()) { "glfwApplication must declare at least one Window" }
    uiDispatcher.bindToCurrentThread()
    initializeGlfw()
    windowSpecs.mapTo(windows) { WindowHost(it, uiDispatcher) }

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
    currentDisplayServer()
  }

  private fun preferredPlatform(): DisplayServer? {
    val requested =
      System.getProperty(PlatformProperty)
        ?: if (System.getenv("WAYLAND_DISPLAY") != null) DisplayServer.WAYLAND.toString() else null
    return requested?.let(DisplayServer::fromSelection)
  }
}

private const val PlatformProperty = "compose.glfw.platform"

private val DisplayServer.glfwPlatformHint: Int
  get() =
    when (this) {
      DisplayServer.WAYLAND -> GLFW_PLATFORM_WAYLAND
      DisplayServer.X11 -> GLFW_PLATFORM_X11
    }

private fun DisplayServer.Companion.fromSelection(value: String): DisplayServer =
  when (value.lowercase()) {
    "wayland" -> DisplayServer.WAYLAND
    "x11" -> DisplayServer.X11
    else -> error("Unsupported GLFW platform '$value'. Use 'wayland' or 'x11'.")
  }
