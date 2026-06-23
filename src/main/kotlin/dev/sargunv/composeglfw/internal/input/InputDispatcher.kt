package dev.sargunv.composeglfw.internal.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import dev.sargunv.composeglfw.internal.platform.TextInputService
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import java.nio.file.Path
import org.lwjgl.glfw.GLFW.GLFW_KEY_CAPS_LOCK
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SUPER
import org.lwjgl.glfw.GLFW.GLFW_KEY_NUM_LOCK
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SUPER
import org.lwjgl.glfw.GLFW.GLFW_KEY_SCROLL_LOCK
import org.lwjgl.glfw.GLFW.GLFW_MOD_ALT
import org.lwjgl.glfw.GLFW.GLFW_MOD_CAPS_LOCK
import org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_MOD_NUM_LOCK
import org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_2
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_3
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_4
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_5
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.GLFW_REPEAT
import org.lwjgl.glfw.GLFW.glfwGetCursorPos
import org.lwjgl.glfw.GLFW.glfwSetCharCallback
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwSetDropCallback
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback
import org.lwjgl.glfw.GLFW.glfwSetScrollCallback
import org.lwjgl.glfw.GLFWDropCallback
import org.lwjgl.system.MemoryStack

// Compose's desktop scroll config normally sees AWT's scrollAmount, commonly 3 lines per wheel
// step.
// GLFW gives unit offsets without that metadata, so apply the same baseline before forwarding.
private const val ScrollAmount = 3f

internal class InputDispatcher(
  private val window: PlatformWindow,
  private val scene: ComposeWindowScene,
  textInput: TextInputService,
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
        sendPointer(
          if (action == GLFW_PRESS) PointerEventType.Press else PointerEventType.Release,
          pointerButton,
        )
      }
    }
    glfwSetScrollCallback(window.handle) { _, x, y ->
      sendPointer(
        type = PointerEventType.Scroll,
        scrollDelta = Offset(x.toFloat(), -y.toFloat()) * ScrollAmount,
      )
    }
    glfwSetKeyCallback(window.handle) { _, key, scancode, action, mods ->
      if (key == GLFW_KEY_SCROLL_LOCK && action == GLFW_PRESS) {
        scrollLockOn = !scrollLockOn
      }
      val normalizedMods = updateKeyEventModifiers(key, action, mods)
      val event = glfwKeyEvent(key, scancode, action, normalizedMods)
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
    // GLFW currently exposes only the final file-drop callback, not a full drag session with
    // enter/move/action/drop events, and current Wayland stacks may not deliver this callback.
    // Full drag-and-drop parity is blocked upstream:
    // https://github.com/glfw/glfw/issues/1898
    glfwSetDropCallback(window.handle) { _, count, names ->
      readMousePosition()
      val paths =
        List(count) { index ->
          Path.of(GLFWDropCallback.getName(names, index))
        }
      scene.sendFileDrop(lastMouse, paths)
      requestRender()
    }
  }

  override fun close() {
    glfwSetCursorPosCallback(window.handle, null)?.free()
    glfwSetMouseButtonCallback(window.handle, null)?.free()
    glfwSetScrollCallback(window.handle, null)?.free()
    glfwSetKeyCallback(window.handle, null)?.free()
    glfwSetCharCallback(window.handle, null)?.free()
    glfwSetDropCallback(window.handle, null)?.free()
  }

  private fun updateMousePosition(x: Double, y: Double) {
    val framebuffer = window.framebufferSize
    val windowSize = window.windowSize
    // GLFW cursor positions are window screen coordinates; Compose local positions are framebuffer
    // pixels.
    lastMouse =
      Offset(
        (x * framebuffer.width / windowSize.width).toFloat(),
        (y * framebuffer.height / windowSize.height).toFloat(),
      )
  }

  private fun readMousePosition() {
    MemoryStack.stackPush().use { stack ->
      val x = stack.mallocDouble(1)
      val y = stack.mallocDouble(1)
      glfwGetCursorPos(window.handle, x, y)
      updateMousePosition(x[0], y[0])
    }
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

  private fun updateKeyboardModifiers(mods: Int): Int {
    currentMods = mods
    onKeyboardModifiers(glfwKeyboardModifiers(currentMods, scrollLockOn))
    return currentMods
  }

  private fun updateKeyEventModifiers(key: Int, action: Int, mods: Int): Int =
    updateKeyboardModifiers(normalizedKeyEventModifiers(key, action, mods))

  private fun normalizedKeyEventModifiers(key: Int, action: Int, mods: Int): Int {
    if (!window.reportsPreEventKeyModifiers) {
      return mods
    }

    // X11 reports the modifier state from just before key events. Compose wants the effective
    // state after the current event, so apply the key action to GLFW's event metadata.
    val pressedModifier = key.glfwPressedModifierMask()
    if (pressedModifier != null) {
      return when (action) {
        GLFW_PRESS,
        GLFW_REPEAT -> mods or pressedModifier
        GLFW_RELEASE -> mods and pressedModifier.inv()
        else -> mods
      }
    }

    val lockModifier = key.glfwLockModifierMask()
    return when {
      lockModifier == null -> mods
      action == GLFW_PRESS -> mods xor lockModifier
      action == GLFW_RELEASE -> mods.withModifier(lockModifier, currentMods has lockModifier)
      else -> mods
    }
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

private fun Int.glfwPressedModifierMask(): Int? =
  when (this) {
    GLFW_KEY_LEFT_SHIFT,
    GLFW_KEY_RIGHT_SHIFT -> GLFW_MOD_SHIFT
    GLFW_KEY_LEFT_CONTROL,
    GLFW_KEY_RIGHT_CONTROL -> GLFW_MOD_CONTROL
    GLFW_KEY_LEFT_ALT,
    GLFW_KEY_RIGHT_ALT -> GLFW_MOD_ALT
    GLFW_KEY_LEFT_SUPER,
    GLFW_KEY_RIGHT_SUPER -> GLFW_MOD_SUPER
    else -> null
  }

private fun Int.glfwLockModifierMask(): Int? =
  when (this) {
    GLFW_KEY_CAPS_LOCK -> GLFW_MOD_CAPS_LOCK
    GLFW_KEY_NUM_LOCK -> GLFW_MOD_NUM_LOCK
    else -> null
  }

private fun Int.withModifier(mask: Int, enabled: Boolean): Int =
  if (enabled) {
    this or mask
  } else {
    this and mask.inv()
  }

private infix fun Int.has(mask: Int): Boolean = (this and mask) != 0
