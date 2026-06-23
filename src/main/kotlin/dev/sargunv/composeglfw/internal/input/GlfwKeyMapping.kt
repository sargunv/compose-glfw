@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import org.lwjgl.glfw.GLFW.GLFW_KEY_0
import org.lwjgl.glfw.GLFW.GLFW_KEY_1
import org.lwjgl.glfw.GLFW.GLFW_KEY_2
import org.lwjgl.glfw.GLFW.GLFW_KEY_3
import org.lwjgl.glfw.GLFW.GLFW_KEY_4
import org.lwjgl.glfw.GLFW.GLFW_KEY_5
import org.lwjgl.glfw.GLFW.GLFW_KEY_6
import org.lwjgl.glfw.GLFW.GLFW_KEY_7
import org.lwjgl.glfw.GLFW.GLFW_KEY_8
import org.lwjgl.glfw.GLFW.GLFW_KEY_9
import org.lwjgl.glfw.GLFW.GLFW_KEY_A
import org.lwjgl.glfw.GLFW.GLFW_KEY_APOSTROPHE
import org.lwjgl.glfw.GLFW.GLFW_KEY_B
import org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSLASH
import org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE
import org.lwjgl.glfw.GLFW.GLFW_KEY_C
import org.lwjgl.glfw.GLFW.GLFW_KEY_CAPS_LOCK
import org.lwjgl.glfw.GLFW.GLFW_KEY_COMMA
import org.lwjgl.glfw.GLFW.GLFW_KEY_D
import org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
import org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN
import org.lwjgl.glfw.GLFW.GLFW_KEY_E
import org.lwjgl.glfw.GLFW.GLFW_KEY_END
import org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
import org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_KEY_F
import org.lwjgl.glfw.GLFW.GLFW_KEY_F1
import org.lwjgl.glfw.GLFW.GLFW_KEY_F10
import org.lwjgl.glfw.GLFW.GLFW_KEY_F11
import org.lwjgl.glfw.GLFW.GLFW_KEY_F12
import org.lwjgl.glfw.GLFW.GLFW_KEY_F13
import org.lwjgl.glfw.GLFW.GLFW_KEY_F14
import org.lwjgl.glfw.GLFW.GLFW_KEY_F15
import org.lwjgl.glfw.GLFW.GLFW_KEY_F16
import org.lwjgl.glfw.GLFW.GLFW_KEY_F17
import org.lwjgl.glfw.GLFW.GLFW_KEY_F18
import org.lwjgl.glfw.GLFW.GLFW_KEY_F19
import org.lwjgl.glfw.GLFW.GLFW_KEY_F2
import org.lwjgl.glfw.GLFW.GLFW_KEY_F20
import org.lwjgl.glfw.GLFW.GLFW_KEY_F21
import org.lwjgl.glfw.GLFW.GLFW_KEY_F22
import org.lwjgl.glfw.GLFW.GLFW_KEY_F23
import org.lwjgl.glfw.GLFW.GLFW_KEY_F24
import org.lwjgl.glfw.GLFW.GLFW_KEY_F25
import org.lwjgl.glfw.GLFW.GLFW_KEY_F3
import org.lwjgl.glfw.GLFW.GLFW_KEY_F4
import org.lwjgl.glfw.GLFW.GLFW_KEY_F5
import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_KEY_F7
import org.lwjgl.glfw.GLFW.GLFW_KEY_F8
import org.lwjgl.glfw.GLFW.GLFW_KEY_F9
import org.lwjgl.glfw.GLFW.GLFW_KEY_G
import org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT
import org.lwjgl.glfw.GLFW.GLFW_KEY_H
import org.lwjgl.glfw.GLFW.GLFW_KEY_HOME
import org.lwjgl.glfw.GLFW.GLFW_KEY_I
import org.lwjgl.glfw.GLFW.GLFW_KEY_INSERT
import org.lwjgl.glfw.GLFW.GLFW_KEY_J
import org.lwjgl.glfw.GLFW.GLFW_KEY_K
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_1
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_2
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_3
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_4
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_5
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_6
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_7
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_8
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_9
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_DECIMAL
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_DIVIDE
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_EQUAL
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_MULTIPLY
import org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT
import org.lwjgl.glfw.GLFW.GLFW_KEY_L
import org.lwjgl.glfw.GLFW.GLFW_KEY_LAST
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_BRACKET
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SUPER
import org.lwjgl.glfw.GLFW.GLFW_KEY_M
import org.lwjgl.glfw.GLFW.GLFW_KEY_MENU
import org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS
import org.lwjgl.glfw.GLFW.GLFW_KEY_N
import org.lwjgl.glfw.GLFW.GLFW_KEY_NUM_LOCK
import org.lwjgl.glfw.GLFW.GLFW_KEY_O
import org.lwjgl.glfw.GLFW.GLFW_KEY_P
import org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN
import org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP
import org.lwjgl.glfw.GLFW.GLFW_KEY_PAUSE
import org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD
import org.lwjgl.glfw.GLFW.GLFW_KEY_PRINT_SCREEN
import org.lwjgl.glfw.GLFW.GLFW_KEY_Q
import org.lwjgl.glfw.GLFW.GLFW_KEY_R
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_BRACKET
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SUPER
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
import org.lwjgl.glfw.GLFW.GLFW_KEY_SCROLL_LOCK
import org.lwjgl.glfw.GLFW.GLFW_KEY_SEMICOLON
import org.lwjgl.glfw.GLFW.GLFW_KEY_SLASH
import org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
import org.lwjgl.glfw.GLFW.GLFW_KEY_T
import org.lwjgl.glfw.GLFW.GLFW_KEY_TAB
import org.lwjgl.glfw.GLFW.GLFW_KEY_U
import org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN
import org.lwjgl.glfw.GLFW.GLFW_KEY_UP
import org.lwjgl.glfw.GLFW.GLFW_KEY_V
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.glfw.GLFW.GLFW_KEY_WORLD_1
import org.lwjgl.glfw.GLFW.GLFW_KEY_WORLD_2
import org.lwjgl.glfw.GLFW.GLFW_KEY_X
import org.lwjgl.glfw.GLFW.GLFW_KEY_Y
import org.lwjgl.glfw.GLFW.GLFW_KEY_Z
import org.lwjgl.glfw.GLFW.GLFW_MOD_ALT
import org.lwjgl.glfw.GLFW.GLFW_MOD_CAPS_LOCK
import org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_MOD_NUM_LOCK
import org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.GLFW_REPEAT
import java.awt.event.KeyEvent as AwtKeyEvent

private val glfwKeyCodes = IntArray(GLFW_KEY_LAST + 1) { key -> glfwKeyCodeFromMapping(key) }
private val glfwKeyLocations = IntArray(GLFW_KEY_LAST + 1) { key -> glfwKeyLocationFromMapping(key) }

internal fun glfwKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): KeyEvent? {
  val type =
    when (action) {
      GLFW_PRESS, GLFW_REPEAT -> KeyEventType.KeyDown
      GLFW_RELEASE -> KeyEventType.KeyUp
      else -> return null
    }
  return KeyEvent(
    key = Key(glfwKeyCode(key), glfwKeyLocation(key)),
    type = type,
    codePoint = 0,
    isCtrlPressed = mods has GLFW_MOD_CONTROL,
    isMetaPressed = mods has GLFW_MOD_SUPER,
    isAltPressed = mods has GLFW_MOD_ALT,
    isShiftPressed = mods has GLFW_MOD_SHIFT,
    nativeEvent = GlfwKeyNativeEvent(key, scancode, action, mods),
  )
}

internal data class GlfwKeyNativeEvent(val key: Int, val scancode: Int, val action: Int, val mods: Int)

internal fun glfwKeyboardModifiers(mods: Int, scrollLockOn: Boolean = false): PointerKeyboardModifiers =
  PointerKeyboardModifiers(
    isCtrlPressed = mods has GLFW_MOD_CONTROL,
    isMetaPressed = mods has GLFW_MOD_SUPER,
    isAltPressed = mods has GLFW_MOD_ALT,
    isShiftPressed = mods has GLFW_MOD_SHIFT,
    isCapsLockOn = mods has GLFW_MOD_CAPS_LOCK,
    isScrollLockOn = scrollLockOn,
    isNumLockOn = mods has GLFW_MOD_NUM_LOCK,
  )

private infix fun Int.has(mask: Int): Boolean = (this and mask) != 0

private fun glfwKeyLocation(key: Int): Int =
  glfwKeyLocations.getOrElse(key) { AwtKeyEvent.KEY_LOCATION_STANDARD }

private fun glfwKeyLocationFromMapping(key: Int): Int =
  when (key) {
    GLFW_KEY_LEFT_SHIFT, GLFW_KEY_LEFT_CONTROL, GLFW_KEY_LEFT_ALT, GLFW_KEY_LEFT_SUPER -> AwtKeyEvent.KEY_LOCATION_LEFT
    GLFW_KEY_RIGHT_SHIFT, GLFW_KEY_RIGHT_CONTROL, GLFW_KEY_RIGHT_ALT, GLFW_KEY_RIGHT_SUPER -> AwtKeyEvent.KEY_LOCATION_RIGHT
    GLFW_KEY_NUM_LOCK -> AwtKeyEvent.KEY_LOCATION_NUMPAD
    in GLFW_KEY_KP_0..GLFW_KEY_KP_EQUAL -> AwtKeyEvent.KEY_LOCATION_NUMPAD
    else -> AwtKeyEvent.KEY_LOCATION_STANDARD
  }

private fun glfwKeyCode(key: Int): Int =
  glfwKeyCodes.getOrElse(key) { AwtKeyEvent.VK_UNDEFINED }

private fun glfwKeyCodeFromMapping(key: Int): Int =
  when (key) {
    GLFW_KEY_UNKNOWN -> AwtKeyEvent.VK_UNDEFINED
    GLFW_KEY_SPACE -> AwtKeyEvent.VK_SPACE
    GLFW_KEY_APOSTROPHE -> AwtKeyEvent.VK_QUOTE
    GLFW_KEY_COMMA -> AwtKeyEvent.VK_COMMA
    GLFW_KEY_MINUS -> AwtKeyEvent.VK_MINUS
    GLFW_KEY_PERIOD -> AwtKeyEvent.VK_PERIOD
    GLFW_KEY_SLASH -> AwtKeyEvent.VK_SLASH
    in GLFW_KEY_0..GLFW_KEY_9 -> AwtKeyEvent.VK_0 + (key - GLFW_KEY_0)
    GLFW_KEY_SEMICOLON -> AwtKeyEvent.VK_SEMICOLON
    GLFW_KEY_EQUAL -> AwtKeyEvent.VK_EQUALS
    in GLFW_KEY_A..GLFW_KEY_Z -> AwtKeyEvent.VK_A + (key - GLFW_KEY_A)
    GLFW_KEY_LEFT_BRACKET -> AwtKeyEvent.VK_OPEN_BRACKET
    GLFW_KEY_BACKSLASH -> AwtKeyEvent.VK_BACK_SLASH
    GLFW_KEY_RIGHT_BRACKET -> AwtKeyEvent.VK_CLOSE_BRACKET
    GLFW_KEY_GRAVE_ACCENT -> AwtKeyEvent.VK_BACK_QUOTE
    // GLFW exposes these non-US physical keys, but Compose desktop models keys using AWT key codes,
    // which do not have stable equivalents for them.
    GLFW_KEY_WORLD_1 -> AwtKeyEvent.VK_UNDEFINED
    GLFW_KEY_WORLD_2 -> AwtKeyEvent.VK_UNDEFINED
    GLFW_KEY_ESCAPE -> AwtKeyEvent.VK_ESCAPE
    GLFW_KEY_ENTER -> AwtKeyEvent.VK_ENTER
    GLFW_KEY_TAB -> AwtKeyEvent.VK_TAB
    GLFW_KEY_BACKSPACE -> AwtKeyEvent.VK_BACK_SPACE
    GLFW_KEY_INSERT -> AwtKeyEvent.VK_INSERT
    GLFW_KEY_DELETE -> AwtKeyEvent.VK_DELETE
    GLFW_KEY_RIGHT -> AwtKeyEvent.VK_RIGHT
    GLFW_KEY_LEFT -> AwtKeyEvent.VK_LEFT
    GLFW_KEY_DOWN -> AwtKeyEvent.VK_DOWN
    GLFW_KEY_UP -> AwtKeyEvent.VK_UP
    GLFW_KEY_PAGE_UP -> AwtKeyEvent.VK_PAGE_UP
    GLFW_KEY_PAGE_DOWN -> AwtKeyEvent.VK_PAGE_DOWN
    GLFW_KEY_HOME -> AwtKeyEvent.VK_HOME
    GLFW_KEY_END -> AwtKeyEvent.VK_END
    GLFW_KEY_CAPS_LOCK -> AwtKeyEvent.VK_CAPS_LOCK
    GLFW_KEY_SCROLL_LOCK -> AwtKeyEvent.VK_SCROLL_LOCK
    GLFW_KEY_NUM_LOCK -> AwtKeyEvent.VK_NUM_LOCK
    GLFW_KEY_PRINT_SCREEN -> AwtKeyEvent.VK_PRINTSCREEN
    GLFW_KEY_PAUSE -> AwtKeyEvent.VK_PAUSE
    in GLFW_KEY_F1..GLFW_KEY_F12 -> AwtKeyEvent.VK_F1 + (key - GLFW_KEY_F1)
    GLFW_KEY_F13 -> AwtKeyEvent.VK_F13
    GLFW_KEY_F14 -> AwtKeyEvent.VK_F14
    GLFW_KEY_F15 -> AwtKeyEvent.VK_F15
    GLFW_KEY_F16 -> AwtKeyEvent.VK_F16
    GLFW_KEY_F17 -> AwtKeyEvent.VK_F17
    GLFW_KEY_F18 -> AwtKeyEvent.VK_F18
    GLFW_KEY_F19 -> AwtKeyEvent.VK_F19
    GLFW_KEY_F20 -> AwtKeyEvent.VK_F20
    GLFW_KEY_F21 -> AwtKeyEvent.VK_F21
    GLFW_KEY_F22 -> AwtKeyEvent.VK_F22
    GLFW_KEY_F23 -> AwtKeyEvent.VK_F23
    GLFW_KEY_F24 -> AwtKeyEvent.VK_F24
    // AWT stops at F24.
    GLFW_KEY_F25 -> AwtKeyEvent.VK_UNDEFINED
    in GLFW_KEY_KP_0..GLFW_KEY_KP_9 -> AwtKeyEvent.VK_NUMPAD0 + (key - GLFW_KEY_KP_0)
    GLFW_KEY_KP_DECIMAL -> AwtKeyEvent.VK_PERIOD
    GLFW_KEY_KP_DIVIDE -> AwtKeyEvent.VK_DIVIDE
    GLFW_KEY_KP_MULTIPLY -> AwtKeyEvent.VK_MULTIPLY
    GLFW_KEY_KP_SUBTRACT -> AwtKeyEvent.VK_SUBTRACT
    GLFW_KEY_KP_ADD -> AwtKeyEvent.VK_ADD
    GLFW_KEY_KP_ENTER -> AwtKeyEvent.VK_ENTER
    GLFW_KEY_KP_EQUAL -> AwtKeyEvent.VK_EQUALS
    GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> AwtKeyEvent.VK_SHIFT
    GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> AwtKeyEvent.VK_CONTROL
    GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> AwtKeyEvent.VK_ALT
    GLFW_KEY_LEFT_SUPER, GLFW_KEY_RIGHT_SUPER -> AwtKeyEvent.VK_META
    GLFW_KEY_MENU -> AwtKeyEvent.VK_CONTEXT_MENU
    else -> AwtKeyEvent.VK_UNDEFINED
  }
