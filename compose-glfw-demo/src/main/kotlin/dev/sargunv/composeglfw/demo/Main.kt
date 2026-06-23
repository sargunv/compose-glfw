package dev.sargunv.composeglfw.demo

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.glfwApplication

internal fun main(): Unit {
  glfwApplication {
    Window(title = "Compose GLFW demo", size = DpSize(960.dp, 640.dp)) {
      ComposeGlfwApp(windowInfo)
    }
  }
}
