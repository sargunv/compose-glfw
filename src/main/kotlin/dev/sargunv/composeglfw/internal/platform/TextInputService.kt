@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import org.lwjgl.glfw.GLFW.glfwSetPreeditCursorRectangle

internal class TextInputService {
  private var currentInput: PlatformTextInputMethodRequest? = null
  private var lastPreedit: String = ""

  var onInputMethodStarting: (() -> Unit)? = null
  var onInputMethodActiveChanged: ((Boolean) -> Unit)? = null

  val isComposing: Boolean
    get() = lastPreedit.isNotEmpty()

  val isInputMethodActive: Boolean
    get() = currentInput != null

  suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
    lastPreedit = ""
    onInputMethodStarting?.invoke()
    currentInput = request
    onInputMethodActiveChanged?.invoke(true)
    try {
      awaitCancellation()
    } finally {
      if (currentInput === request) {
        currentInput = null
        lastPreedit = ""
        onInputMethodActiveChanged?.invoke(false)
      }
    }
  }

  fun commit(codePoint: Int): Boolean {
    val input = currentInput ?: return false
    if (!Character.isValidCodePoint(codePoint)) return false

    input.editText {
      commitText(String(Character.toChars(codePoint)), 1)
    }
    lastPreedit = ""
    return true
  }

  fun setPreedit(
    text: String,
    caretCodePointOffset: Int,
  ): Boolean {
    val input = currentInput ?: return false
    lastPreedit = text

    input.editText {
      setComposingText(text, newCursorPosition(text, caretCodePointOffset))
    }
    return true
  }

  fun updatePreeditCursorRectangle(window: PlatformWindow): Boolean {
    val input = currentInput ?: return false
    val focusedRect = input.focusedRectInRoot() ?: return false
    val rectangle = focusedRect.toGlfwWindowRectangle(window)

    runCatching {
      glfwSetPreeditCursorRectangle(
        window.handle,
        rectangle.x,
        rectangle.y,
        rectangle.width,
        rectangle.height,
      )
    }
    return true
  }

  private fun newCursorPosition(
    text: String,
    caretCodePointOffset: Int,
  ): Int {
    val caretCharOffset = text.charOffsetForCodePointOffset(caretCodePointOffset)
    return if (caretCharOffset >= text.length) 1 else caretCharOffset
  }
}

private data class GlfwWindowRectangle(
  val x: Int,
  val y: Int,
  val width: Int,
  val height: Int,
)

private fun Rect.toGlfwWindowRectangle(window: PlatformWindow): GlfwWindowRectangle {
  // Compose roots render in framebuffer pixels. GLFW IME positioning follows window input
  // coordinates, matching cursor positions relative to the content area.
  val xScale = window.windowSize.width.toFloat() / window.framebufferSize.width
  val yScale = window.windowSize.height.toFloat() / window.framebufferSize.height
  return GlfwWindowRectangle(
    x = (left * xScale).roundToInt(),
    y = (top * yScale).roundToInt(),
    width = ((right - left) * xScale).roundToInt().coerceAtLeast(1),
    height = ((bottom - top) * yScale).roundToInt().coerceAtLeast(1),
  )
}

private fun String.charOffsetForCodePointOffset(codePointOffset: Int): Int {
  if (codePointOffset <= 0) return 0
  var charOffset = 0
  var codePoints = 0
  while (charOffset < length && codePoints < codePointOffset) {
    charOffset += Character.charCount(codePointAt(charOffset))
    codePoints++
  }
  return charOffset
}
