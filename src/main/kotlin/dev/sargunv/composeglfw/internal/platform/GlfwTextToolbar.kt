package dev.sargunv.composeglfw.internal.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

internal class GlfwTextToolbar : TextToolbar {
  private var state: TextToolbarState? by mutableStateOf(null)

  override val status: TextToolbarStatus
    get() = if (state == null) TextToolbarStatus.Hidden else TextToolbarStatus.Shown

  override fun showMenu(
    rect: Rect,
    onCopyRequested: (() -> Unit)?,
    onPasteRequested: (() -> Unit)?,
    onCutRequested: (() -> Unit)?,
    onSelectAllRequested: (() -> Unit)?,
  ) {
    state =
      TextToolbarState(
        rect = rect,
        onCopyRequested = onCopyRequested,
        onPasteRequested = onPasteRequested,
        onCutRequested = onCutRequested,
        onSelectAllRequested = onSelectAllRequested,
      )
  }

  override fun hide() {
    state = null
  }

  @Composable
  fun Content() {
    val currentState = state ?: return
    Popup(
      popupPositionProvider = TextToolbarPositionProvider(currentState.rect),
      onDismissRequest = ::hide,
      properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
    ) {
      Row(
        Modifier
          .background(TextToolbarBackground, RoundedCornerShape(6.dp))
          .padding(horizontal = 4.dp, vertical = 3.dp),
      ) {
        TextToolbarAction("Cut", currentState.onCutRequested)
        TextToolbarAction("Copy", currentState.onCopyRequested)
        TextToolbarAction("Paste", currentState.onPasteRequested)
        TextToolbarAction("Select all", currentState.onSelectAllRequested)
      }
    }
  }

  @Composable
  private fun TextToolbarAction(label: String, action: (() -> Unit)?) {
    if (action == null) {
      return
    }
    BasicText(
      text = label,
      modifier =
        Modifier
          .clickable {
            action()
            hide()
          }
          .padding(horizontal = 10.dp, vertical = 7.dp),
      style = TextStyle(color = TextToolbarContent),
    )
  }
}

private data class TextToolbarState(
  val rect: Rect,
  val onCopyRequested: (() -> Unit)?,
  val onPasteRequested: (() -> Unit)?,
  val onCutRequested: (() -> Unit)?,
  val onSelectAllRequested: (() -> Unit)?,
)

private class TextToolbarPositionProvider(private val rect: Rect) : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset {
    val x = rect.left.roundToInt().fitWithin(windowSize.width, popupContentSize.width)
    val below = rect.bottom.roundToInt()
    val above = rect.top.roundToInt() - popupContentSize.height
    val y =
      if (below + popupContentSize.height <= windowSize.height) {
        below
      } else {
        above
      }.fitWithin(windowSize.height, popupContentSize.height)
    return IntOffset(x, y)
  }
}

private fun Int.fitWithin(containerSize: Int, contentSize: Int): Int =
  if (contentSize < containerSize) {
    coerceIn(0, containerSize - contentSize)
  } else {
    0
  }

private val TextToolbarBackground = Color(0xFF2B2B2B)
private val TextToolbarContent = Color(0xFFFFFFFF)
