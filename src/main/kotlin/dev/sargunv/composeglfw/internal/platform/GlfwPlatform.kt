package dev.sargunv.composeglfw.internal.platform

import dev.sargunv.composeglfw.GlfwPlatform
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
import org.lwjgl.glfw.GLFW.glfwGetPlatform

internal fun glfwPlatform(): GlfwPlatform =
  when (glfwGetPlatform()) {
    GLFW_PLATFORM_WAYLAND -> GlfwPlatform.WAYLAND
    else -> error("Only GLFW Wayland is supported; GLFW selected ${glfwPlatformName()}")
  }

internal fun glfwPlatformName(): String =
  when (glfwGetPlatform()) {
    GLFW_PLATFORM_WAYLAND -> "Wayland"
    GLFW_PLATFORM_X11 -> "X11"
    else -> "Unknown"
  }
