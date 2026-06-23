package dev.sargunv.composeglfw.internal.platform

import dev.sargunv.composeglfw.DisplayServer
import org.lwjgl.glfw.GLFW.glfwGetPlatform

internal fun currentDisplayServer(): DisplayServer =
  DisplayServer.fromGlfwPlatform(glfwGetPlatform())

internal fun DisplayServer.displayName(): String? =
  when (this) {
    DisplayServer.WAYLAND -> System.getenv("WAYLAND_DISPLAY")
    DisplayServer.X11 -> System.getenv("DISPLAY")
  }
