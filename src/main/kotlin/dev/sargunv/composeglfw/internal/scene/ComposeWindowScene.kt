@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.sargunv.composeglfw.GlfwWindowScope
import dev.sargunv.composeglfw.internal.platform.GlfwPlatformContext
import kotlin.coroutines.CoroutineContext

internal class ComposeWindowScene(
  initialDensity: Float,
  initialSize: IntSize,
  platformContext: GlfwPlatformContext,
  coroutineContext: CoroutineContext,
  scope: GlfwWindowScope,
  content: @Composable GlfwWindowScope.() -> Unit,
  invalidate: () -> Unit,
) : AutoCloseable {
  private val scene: ComposeScene =
    CanvasLayersComposeScene(
      density = Density(initialDensity),
      layoutDirection = LayoutDirection.Ltr,
      // The scene is rendered directly into the Skia framebuffer target, so local coordinates are pixels.
      size = initialSize,
      platformContext = platformContext,
      coroutineContext = coroutineContext,
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

  fun resize(size: IntSize) {
    scene.size = size
  }

  fun updateDensity(density: Float) {
    if (scene.density.density != density) {
      scene.density = Density(density)
    }
  }

  fun sendKeyEvent(event: KeyEvent): Boolean = scene.sendKeyEvent(event)

  fun sendPointerEvent(
    event: PointerEventType,
    // Compose expects this in the same framebuffer-pixel local space as scene.size.
    position: Offset,
    scrollDelta: Offset = Offset.Zero,
    button: PointerButton?,
    buttons: PointerButtons,
    keyboardModifiers: PointerKeyboardModifiers,
  ) {
    scene.sendPointerEvent(
      eventType = event,
      position = position,
      scrollDelta = scrollDelta,
      timeMillis = System.currentTimeMillis(),
      type = PointerType.Mouse,
      buttons = buttons,
      keyboardModifiers = keyboardModifiers,
      nativeEvent = null,
      button = button,
    )
  }

  override fun close() {
    scene.close()
  }
}
