package dev.sargunv.composeglfw.internal.platform

import dev.sargunv.composeglfw.GlfwPlatform
import org.lwjgl.glfw.GLFW.glfwGetPlatform

internal fun glfwPlatform(): GlfwPlatform =
  GlfwPlatform.fromGlfwPlatform(glfwGetPlatform())

internal fun GlfwPlatform.glfwDisplayName(): String? =
  when (this) {
    GlfwPlatform.WAYLAND -> System.getenv("WAYLAND_DISPLAY")
    GlfwPlatform.X11 -> System.getenv("DISPLAY")
  }
