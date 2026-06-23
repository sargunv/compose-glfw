package dev.sargunv.composeglfw.demo

import dev.sargunv.composeglfw.GlfwWindowSize
import dev.sargunv.composeglfw.glfwApplication

fun main() {
  glfwApplication {
    Window(title = "Compose GLFW demo", size = GlfwWindowSize(960, 640)) {
      ComposeGlfwApp(windowInfo)
    }
  }
}
