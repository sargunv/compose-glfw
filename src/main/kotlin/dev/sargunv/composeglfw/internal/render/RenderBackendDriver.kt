package dev.sargunv.composeglfw.internal.render

import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene

internal interface RenderBackendDriver : AutoCloseable {
  fun resize(size: IntSize)

  fun render(scene: ComposeWindowScene, frameTimeNanos: Long)

  fun renderWithoutPresenting(scene: ComposeWindowScene, frameTimeNanos: Long)
}
