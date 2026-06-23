package dev.sargunv.composeglfw.demo

import dev.sargunv.composeglfw.WindowSize
import dev.sargunv.composeglfw.glfwApplication

internal fun main(): Unit {
  glfwApplication {
    Window(title = "Compose GLFW demo", size = WindowSize(960, 640)) {
      ComposeGlfwApp(windowInfo)
    }
  }
}
