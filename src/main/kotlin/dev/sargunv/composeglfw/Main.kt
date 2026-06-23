package dev.sargunv.composeglfw

fun main() {
  GlfwComposeHost("Compose GLFW proof of concept", 960, 640).use { it.run() }
}
