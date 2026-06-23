package dev.sargunv.composeglfw.internal.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import dev.sargunv.composeglfw.internal.platform.GlfwTextInputService
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.GlfwPlatformWindow
import org.lwjgl.glfw.GLFW.GLFW_KEY_SCROLL_LOCK
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_2
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_3
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_4
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_5
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.glfwSetCharCallback
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback
import org.lwjgl.glfw.GLFW.glfwSetScrollCallback

// Compose's desktop scroll config normally sees AWT's scrollAmount, commonly 3 lines per wheel step.
// GLFW gives unit offsets without that metadata, so apply the same baseline before forwarding.
private const val GlfwScrollAmount = 3f

internal class GlfwInputDispatcher(
  private val window: GlfwPlatformWindow,
  private val scene: ComposeWindowScene,
  textInput: GlfwTextInputService,
  private val onKeyboardModifiers: (PointerKeyboardModifiers) -> Unit,
  private val requestRender: () -> Unit,
) : AutoCloseable {
  private var pressedMouseButtons = 0
  private var lastMouse = Offset.Zero
  private var currentMods = 0
  private var scrollLockOn = false

  init {
    glfwSetCursorPosCallback(window.handle) { _, x, y ->
      updateMousePosition(x, y)
      sendPointer(PointerEventType.Move)
    }
    glfwSetMouseButtonCallback(window.handle) { _, button, action, mods ->
      updateKeyboardModifiers(mods)
      val pointerButton = button.toPointerButton()
      if (pointerButton != null && (action == GLFW_PRESS || action == GLFW_RELEASE)) {
        updateMouseButton(button, action == GLFW_PRESS)
        sendPointer(if (action == GLFW_PRESS) PointerEventType.Press else PointerEventType.Release, pointerButton)
      }
    }
    glfwSetScrollCallback(window.handle) { _, x, y ->
      sendPointer(
        type = PointerEventType.Scroll,
        scrollDelta = Offset(x.toFloat(), -y.toFloat()) * GlfwScrollAmount,
      )
    }
    glfwSetKeyCallback(window.handle) { _, key, scancode, action, mods ->
      if (key == GLFW_KEY_SCROLL_LOCK && action == GLFW_PRESS) {
        scrollLockOn = !scrollLockOn
      }
      updateKeyboardModifiers(mods)
      val event = glfwKeyEvent(key, scancode, action, mods)
      if (event != null) {
        scene.sendKeyEvent(event)
        requestRender()
      }
    }
    glfwSetCharCallback(window.handle) { _, codePoint ->
      if (textInput.commit(codePoint)) {
        requestRender()
      }
    }
  }

  override fun close() {
    glfwSetCursorPosCallback(window.handle, null)?.free()
    glfwSetMouseButtonCallback(window.handle, null)?.free()
    glfwSetScrollCallback(window.handle, null)?.free()
    glfwSetKeyCallback(window.handle, null)?.free()
    glfwSetCharCallback(window.handle, null)?.free()
  }

  private fun updateMousePosition(x: Double, y: Double) {
    val framebuffer = window.framebufferSize
    val windowSize = window.windowSize
    // GLFW cursor positions are window screen coordinates; Compose local positions are framebuffer pixels.
    lastMouse =
      Offset(
        (x * framebuffer.width / windowSize.width).toFloat(),
        (y * framebuffer.height / windowSize.height).toFloat(),
      )
  }

  private fun sendPointer(
    type: PointerEventType,
    button: PointerButton? = null,
    scrollDelta: Offset = Offset.Zero,
  ) {
    scene.sendPointerEvent(
      event = type,
      position = lastMouse,
      scrollDelta = scrollDelta,
      button = button,
      buttons = pointerButtons(),
      keyboardModifiers = glfwKeyboardModifiers(currentMods, scrollLockOn),
    )
    requestRender()
  }

  private fun updateMouseButton(button: Int, pressed: Boolean) {
    val mask = 1 shl button
    pressedMouseButtons =
      if (pressed) {
        pressedMouseButtons or mask
      } else {
        pressedMouseButtons and mask.inv()
      }
  }

  private fun pointerButtons(): PointerButtons =
    PointerButtons(
      isPrimaryPressed = isMouseButtonPressed(GLFW_MOUSE_BUTTON_1),
      isSecondaryPressed = isMouseButtonPressed(GLFW_MOUSE_BUTTON_2),
      isTertiaryPressed = isMouseButtonPressed(GLFW_MOUSE_BUTTON_3),
      isBackPressed = isMouseButtonPressed(GLFW_MOUSE_BUTTON_4),
      isForwardPressed = isMouseButtonPressed(GLFW_MOUSE_BUTTON_5),
    )

  private fun isMouseButtonPressed(button: Int): Boolean =
    pressedMouseButtons and (1 shl button) != 0

  private fun updateKeyboardModifiers(mods: Int) {
    currentMods = mods
    onKeyboardModifiers(glfwKeyboardModifiers(currentMods, scrollLockOn))
  }
}

private fun Int.toPointerButton(): PointerButton? =
  when (this) {
    GLFW_MOUSE_BUTTON_1 -> PointerButton.Primary
    GLFW_MOUSE_BUTTON_2 -> PointerButton.Secondary
    GLFW_MOUSE_BUTTON_3 -> PointerButton.Tertiary
    GLFW_MOUSE_BUTTON_4 -> PointerButton.Back
    GLFW_MOUSE_BUTTON_5 -> PointerButton.Forward
    else -> null
  }
