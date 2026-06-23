@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.sargunv.composeglfw.GlfwWindowScope
import dev.sargunv.composeglfw.internal.window.FramebufferSize
import kotlin.coroutines.EmptyCoroutineContext

internal class ComposeWindowScene(
  initialDensity: Float,
  initialSize: FramebufferSize,
  private val scope: GlfwWindowScope,
  private val content: @Composable GlfwWindowScope.() -> Unit,
  invalidate: () -> Unit,
) : AutoCloseable {
  private val scene: ComposeScene =
    CanvasLayersComposeScene(
      density = Density(initialDensity),
      layoutDirection = LayoutDirection.Ltr,
      size = IntSize(initialSize.width, initialSize.height),
      coroutineContext = EmptyCoroutineContext,
      invalidate = invalidate,
    )

  init {
    scene.setContent { scope.content() }
  }

  val hasInvalidations: Boolean
    get() = scene.hasInvalidations()

  fun render(canvas: Canvas, frameTimeNanos: Long) {
    scene.render(canvas, frameTimeNanos)
  }

  fun resize(size: FramebufferSize) {
    scene.size = IntSize(size.width, size.height)
  }

  fun updateDensity(density: Float) {
    if (scene.density.density != density) {
      scene.density = Density(density)
    }
  }

  fun sendPointerEvent(
    event: androidx.compose.ui.input.pointer.PointerEventType,
    position: androidx.compose.ui.geometry.Offset,
    button: androidx.compose.ui.input.pointer.PointerButton?,
    buttons: androidx.compose.ui.input.pointer.PointerButtons,
  ) {
    scene.sendPointerEvent(
      eventType = event,
      position = position,
      timeMillis = System.currentTimeMillis(),
      type = androidx.compose.ui.input.pointer.PointerType.Mouse,
      buttons = buttons,
      keyboardModifiers = androidx.compose.ui.input.pointer.PointerKeyboardModifiers(0),
      nativeEvent = null,
      button = button,
    )
  }

  override fun close() {
    scene.close()
  }
}
