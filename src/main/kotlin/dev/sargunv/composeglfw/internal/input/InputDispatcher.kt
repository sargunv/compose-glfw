package dev.sargunv.composeglfw.internal.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import dev.sargunv.composeglfw.internal.platform.TextInputService
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import java.awt.Component
import java.awt.event.MouseWheelEvent
import java.nio.file.Path
import kotlin.math.abs
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_IME
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
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.glfwGetCursorPos
import org.lwjgl.glfw.GLFW.glfwResetPreeditText
import org.lwjgl.glfw.GLFW.glfwSetCharCallback
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwSetDropCallback
import org.lwjgl.glfw.GLFW.glfwSetIMEStatusCallback
import org.lwjgl.glfw.GLFW.glfwSetInputMode
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback
import org.lwjgl.glfw.GLFW.glfwSetPreeditCallback
import org.lwjgl.glfw.GLFW.glfwSetScrollCallback
import org.lwjgl.glfw.GLFWDropCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memIntBuffer

private const val WheelScrollAmount = 1
private const val PreciseWheelRotation = 0

internal class InputDispatcher(
  private val window: PlatformWindow,
  private val scene: ComposeWindowScene,
  private val textInput: TextInputService,
  private val onKeyboardModifiers: (PointerKeyboardModifiers) -> Unit,
  private val onKeyboardInputMode: () -> Unit,
  private val onPointerInputMode: () -> Unit,
  private val onPreviewKeyEvent: (KeyEvent) -> Boolean,
  private val onKeyEvent: (KeyEvent) -> Boolean,
  private val requestRender: () -> Unit,
) : AutoCloseable {
  var enabled: Boolean = true
  private var pressedMouseButtons = 0
  private var lastMouse = Offset.Zero
  private var currentMods = 0
  private var scrollLockOn = false
  private val onInputMethodStarting: () -> Unit = {
    resetPreedit()
  }
  private val onInputMethodActiveChanged: (Boolean) -> Unit = { active ->
    updateInputMethodSession(active)
  }

  init {
    installImeCallbacks()
    textInput.onInputMethodStarting = onInputMethodStarting
    textInput.onInputMethodActiveChanged = onInputMethodActiveChanged
    glfwSetCursorPosCallback(window.handle) { _, x, y ->
      if (!enabled) return@glfwSetCursorPosCallback
      updateMousePosition(x, y)
      sendPointer(PointerEventType.Move)
    }
    glfwSetMouseButtonCallback(window.handle) { _, button, action, mods ->
      if (!enabled) return@glfwSetMouseButtonCallback
      updateKeyboardModifiers(mods)
      val pointerButton = button.toPointerButton()
      if (pointerButton != null && (action == GLFW_PRESS || action == GLFW_RELEASE)) {
        onPointerInputMode()
        updateMouseButton(button, action == GLFW_PRESS)
        sendPointer(
          if (action == GLFW_PRESS) PointerEventType.Press else PointerEventType.Release,
          pointerButton,
        )
      }
    }
    glfwSetScrollCallback(window.handle) { _, x, y ->
      if (!enabled) return@glfwSetScrollCallback
      sendPointer(
        type = PointerEventType.Scroll,
        scrollDelta = Offset(x.toFloat(), -y.toFloat()),
        nativeEvent = glfwMouseWheelEvent(x, y),
      )
    }
    glfwSetKeyCallback(window.handle) { _, key, scancode, action, mods ->
      if (!enabled) return@glfwSetKeyCallback
      if (key == GLFW_KEY_SCROLL_LOCK && action == GLFW_PRESS) {
        scrollLockOn = !scrollLockOn
      }
      val normalizedMods = updateKeyEventModifiers(key, action, mods)
      val event = glfwKeyEvent(key, scancode, action, normalizedMods)
      if (event != null) {
        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
          onKeyboardInputMode()
        }
        if (!shouldSuppressKeyEventForIme(key)) {
          onPreviewKeyEvent(event) || scene.sendKeyEvent(event) || onKeyEvent(event)
        }
        requestRender()
      }
    }
    glfwSetCharCallback(window.handle) { _, codePoint ->
      if (!enabled) return@glfwSetCharCallback
      if (textInput.commit(codePoint)) {
        onKeyboardInputMode()
        requestRender()
      }
    }
    // GLFW currently exposes only the final file-drop callback, not a full drag session with
    // enter/move/action/drop events, and current Wayland stacks may not deliver this callback.
    // Full drag-and-drop parity is blocked upstream:
    // https://github.com/glfw/glfw/issues/1898
    glfwSetDropCallback(window.handle) { _, count, names ->
      if (!enabled) return@glfwSetDropCallback
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
    runCatching { glfwSetPreeditCallback(window.handle, null)?.free() }
    runCatching { glfwSetIMEStatusCallback(window.handle, null)?.free() }
    if (textInput.onInputMethodStarting === onInputMethodStarting) {
      textInput.onInputMethodStarting = null
    }
    if (textInput.onInputMethodActiveChanged === onInputMethodActiveChanged) {
      textInput.onInputMethodActiveChanged = null
    }
    runCatching { glfwSetInputMode(window.handle, GLFW_IME, GLFW_FALSE) }
    glfwSetDropCallback(window.handle, null)?.free()
  }

  fun updatePreeditCursorRectangle() {
    textInput.updatePreeditCursorRectangle(window)
  }

  private fun installImeCallbacks() {
    runCatching { glfwSetInputMode(window.handle, GLFW_IME, GLFW_TRUE) }
    runCatching {
      glfwSetPreeditCallback(window.handle) { _, preeditCount, preeditString, _, _, _, caret ->
        if (!enabled) return@glfwSetPreeditCallback
        if (!textInput.isInputMethodActive) {
          return@glfwSetPreeditCallback
        }
        if (textInput.setPreedit(preeditString.toCodePointString(preeditCount), caret)) {
          textInput.updatePreeditCursorRectangle(window)
          onKeyboardInputMode()
          requestRender()
        }
      }
    }
    runCatching {
      glfwSetIMEStatusCallback(window.handle) { _ ->
        if (!enabled) return@glfwSetIMEStatusCallback
        if (textInput.isInputMethodActive) {
          textInput.updatePreeditCursorRectangle(window)
        }
        requestRender()
      }
    }
  }

  private fun updateInputMethodSession(active: Boolean) {
    if (!active) {
      resetPreedit()
    } else {
      textInput.updatePreeditCursorRectangle(window)
    }
  }

  private fun resetPreedit() {
    runCatching { glfwResetPreeditText(window.handle) }
  }

  private fun shouldSuppressKeyEventForIme(key: Int): Boolean =
    textInput.isComposing && !key.isModifierKey()

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
    nativeEvent: Any? = null,
  ) {
    scene.sendPointerEvent(
      event = type,
      position = lastMouse,
      scrollDelta = scrollDelta,
      button = button,
      buttons = pointerButtons(),
      keyboardModifiers = glfwKeyboardModifiers(currentMods, scrollLockOn),
      nativeEvent = nativeEvent,
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

private val ScrollEventSource = object : Component() {}

private fun glfwMouseWheelEvent(
  x: Double,
  y: Double,
): MouseWheelEvent {
  // TODO: GLFW's public scroll callback does not expose input source, precise-wheel state,
  // scroll phase/momentum, or OS scrollAmount. Synthesize the subset of AWT MouseWheelEvent
  // metadata Compose Desktop uses and mark GLFW scroll events as precise until there is native
  // metadata to classify them more accurately.
  val preciseWheelRotation = if (abs(x) > abs(y)) x else y
  return MouseWheelEvent(
    ScrollEventSource,
    MouseWheelEvent.MOUSE_WHEEL,
    System.currentTimeMillis(),
    0,
    0,
    0,
    0,
    0,
    0,
    false,
    MouseWheelEvent.WHEEL_UNIT_SCROLL,
    WheelScrollAmount,
    PreciseWheelRotation,
    preciseWheelRotation,
  )
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

private fun Int.isModifierKey(): Boolean =
  when (this) {
    GLFW_KEY_LEFT_SHIFT,
    GLFW_KEY_RIGHT_SHIFT,
    GLFW_KEY_LEFT_CONTROL,
    GLFW_KEY_RIGHT_CONTROL,
    GLFW_KEY_LEFT_ALT,
    GLFW_KEY_RIGHT_ALT,
    GLFW_KEY_LEFT_SUPER,
    GLFW_KEY_RIGHT_SUPER,
    GLFW_KEY_CAPS_LOCK,
    GLFW_KEY_NUM_LOCK,
    GLFW_KEY_SCROLL_LOCK -> true
    else -> false
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

private fun Long.toCodePointString(count: Int): String {
  if (this == NULL || count <= 0) {
    return ""
  }
  val codePoints = memIntBuffer(this, count)
  return buildString {
    for (index in 0 until count) {
      val codePoint = codePoints[index]
      if (Character.isValidCodePoint(codePoint)) {
        appendCodePoint(codePoint)
      }
    }
  }
}
