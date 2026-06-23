@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.LocalSystemTheme
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
  private val platformContext: GlfwPlatformContext,
  coroutineContext: CoroutineContext,
  scope: GlfwWindowScope,
  content: @Composable GlfwWindowScope.() -> Unit,
  invalidate: () -> Unit,
  private val checkThread: (String) -> Unit,
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
    checkSceneThread("ComposeScene setContent")
    scene.setContent {
      CompositionLocalProvider(LocalSystemTheme provides platformContext.systemTheme) {
        scope.content()
        platformContext.TextToolbarContent()
      }
    }
  }

  val hasInvalidations: Boolean
    get() {
      checkSceneThread("ComposeScene invalidation check")
      return scene.hasInvalidations()
    }

  fun render(canvas: Canvas, frameTimeNanos: Long) {
    checkSceneThread("ComposeScene render")
    scene.render(canvas, frameTimeNanos)
  }

  fun resize(size: IntSize) {
    checkSceneThread("ComposeScene resize")
    scene.size = size
  }

  fun updateDensity(density: Float) {
    checkSceneThread("ComposeScene density update")
    if (scene.density.density != density) {
      scene.density = Density(density)
    }
  }

  fun sendKeyEvent(event: KeyEvent): Boolean {
    checkSceneThread("ComposeScene key event")
    return scene.sendKeyEvent(event)
  }

  fun sendPointerEvent(
    event: PointerEventType,
    // Compose expects this in the same framebuffer-pixel local space as scene.size.
    position: Offset,
    scrollDelta: Offset = Offset.Zero,
    button: PointerButton?,
    buttons: PointerButtons,
    keyboardModifiers: PointerKeyboardModifiers,
  ) {
    checkSceneThread("ComposeScene pointer event")
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
    checkSceneThread("ComposeScene close")
    scene.close()
  }

  private fun checkSceneThread(operation: String) {
    // TODO(CMP-10289): Compose UI 1.11.x starts GlobalSnapshotManager on Skiko's Swing
    // MainUIDispatcher instead of this scene's coroutineContext. Keep this local guard as a
    // diagnostic until snapshot apply notifications are dispatched per scene context.
    // https://youtrack.jetbrains.com/issue/CMP-10289
    checkThread(operation)
  }
}
