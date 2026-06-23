@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import kotlinx.coroutines.awaitCancellation

internal class GlfwTextInputService {
  private var currentInput: CurrentInput? = null

  suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
    val input = CurrentInput(request)
    currentInput = input
    try {
      awaitCancellation()
    } finally {
      if (currentInput === input) {
        currentInput = null
      }
    }
  }

  fun commit(codePoint: Int): Boolean {
    val input = currentInput ?: return false
    if (!Character.isValidCodePoint(codePoint)) return false

    input.request.editText {
      commitText(String(Character.toChars(codePoint)), 1)
    }
    return true
  }

  private data class CurrentInput(val request: PlatformTextInputMethodRequest)
}
