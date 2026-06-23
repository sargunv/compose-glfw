package dev.sargunv.composeglfw.demo

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.Window
import dev.sargunv.composeglfw.glfwApplication
import dev.sargunv.composeglfw.rememberWindowState

internal fun main(): Unit {
  glfwApplication {
    Window(
      onCloseRequest = ::exitApplication,
      title = "Compose GLFW demo",
      state = rememberWindowState(size = DpSize(960.dp, 640.dp)),
    ) {
      ComposeGlfwApp(windowInfo)
    }
  }
}
