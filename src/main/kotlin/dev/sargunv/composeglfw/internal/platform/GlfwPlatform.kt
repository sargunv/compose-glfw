package dev.sargunv.composeglfw.internal.platform

import dev.sargunv.composeglfw.GlfwPlatform
import org.lwjgl.glfw.GLFW.glfwGetPlatform

internal fun glfwPlatform(): GlfwPlatform =
  GlfwPlatform.fromGlfwPlatform(glfwGetPlatform())
