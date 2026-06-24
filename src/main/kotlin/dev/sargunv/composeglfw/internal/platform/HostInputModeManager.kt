package dev.sargunv.composeglfw.internal.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager

internal class HostInputModeManager : InputModeManager {
  override var inputMode: InputMode by mutableStateOf(InputMode.Keyboard)
    private set

  override fun requestInputMode(inputMode: InputMode): Boolean =
    when (inputMode) {
      InputMode.Keyboard -> {
        this.inputMode = InputMode.Keyboard
        true
      }
      InputMode.Touch -> {
        switchToPointerInputMode()
        true
      }
      else -> false
    }

  fun switchToKeyboardInputMode() {
    inputMode = InputMode.Keyboard
  }

  fun switchToPointerInputMode() {
    // Compose uses InputMode.Touch as the non-keyboard pointer interaction mode. GLFW currently
    // exposes mouse input but no native touch/stylus event stream, so mouse button interaction maps
    // to Touch here to match Compose focus-indication behavior.
    // TODO: When touch/stylus events are wired through platform-native APIs, switch this state from
    // actual non-mouse pointer events and route PointerType.Touch/PointerType.Stylus to Compose.
    inputMode = InputMode.Touch
  }
}
