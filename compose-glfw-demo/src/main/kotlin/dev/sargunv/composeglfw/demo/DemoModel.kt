package dev.sargunv.composeglfw.demo

import dev.sargunv.composeglfw.GlfwPlatform
import dev.sargunv.composeglfw.GlfwRenderBackend

internal val GlfwPlatform.displayLabel: String
  get() =
    when (this) {
      GlfwPlatform.WAYLAND -> "Wayland"
    }

internal val GlfwRenderBackend.displayLabel: String
  get() =
    when (this) {
      GlfwRenderBackend.OPENGL -> "OpenGL"
    }
