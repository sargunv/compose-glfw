@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import kotlinx.coroutines.awaitCancellation

internal class TextInputService {
  private var currentInput: PlatformTextInputMethodRequest? = null

  suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
    currentInput = request
    try {
      awaitCancellation()
    } finally {
      if (currentInput === request) {
        currentInput = null
      }
    }
  }

  fun commit(codePoint: Int): Boolean {
    val input = currentInput ?: return false
    if (!Character.isValidCodePoint(codePoint)) return false

    input.editText {
      commitText(String(Character.toChars(codePoint)), 1)
    }
    return true
  }
}
