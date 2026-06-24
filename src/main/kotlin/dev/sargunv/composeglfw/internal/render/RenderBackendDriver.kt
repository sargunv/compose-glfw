package dev.sargunv.composeglfw.internal.render

import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.RenderBackend
import dev.sargunv.composeglfw.RenderContext
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene

internal interface RenderBackendDriver : AutoCloseable {
  val backend: RenderBackend

  val interop: RenderContext

  fun resize(size: IntSize)

  fun render(scene: ComposeWindowScene, frameTimeNanos: Long)

  fun prepareForPreferredSizeMeasurement(scene: ComposeWindowScene, frameTimeNanos: Long) = Unit
}
