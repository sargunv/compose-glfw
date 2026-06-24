package dev.sargunv.composeglfw.internal.platform

import dev.sargunv.composeglfw.DisplayServer
import org.lwjgl.glfw.GLFW.glfwGetPlatform

internal fun currentDisplayServer(): DisplayServer =
  DisplayServer.fromGlfwPlatform(glfwGetPlatform())
