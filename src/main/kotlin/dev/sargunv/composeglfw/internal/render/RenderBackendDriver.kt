package dev.sargunv.composeglfw.internal.render

import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.GpuInterop
import dev.sargunv.composeglfw.RenderBackend
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene

internal interface RenderBackendDriver : AutoCloseable {
  val backend: RenderBackend

  val interop: GpuInterop

  fun resize(size: IntSize)

  fun render(scene: ComposeWindowScene, frameTimeNanos: Long)

  // TODO only wayland implements this so far; reconsider our abstraction
  fun renderWithoutPresenting(scene: ComposeWindowScene, frameTimeNanos: Long)
}
