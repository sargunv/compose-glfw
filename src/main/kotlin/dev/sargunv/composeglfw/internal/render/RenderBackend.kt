package dev.sargunv.composeglfw.internal.render

import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.FramebufferSize

internal interface RenderBackend : AutoCloseable {
  fun resize(size: FramebufferSize)

  fun render(scene: ComposeWindowScene, frameTimeNanos: Long)
}
