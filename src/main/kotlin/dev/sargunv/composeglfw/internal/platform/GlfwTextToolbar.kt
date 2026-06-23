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
import dev.sargunv.composeglfw.GlfwTextToolbarActions
import dev.sargunv.composeglfw.GlfwTextToolbarContent
import dev.sargunv.composeglfw.GlfwTextToolbarState
import kotlin.math.roundToInt

internal class GlfwTextToolbar(private val content: GlfwTextToolbarContent) : TextToolbar {
  private var request: TextToolbarRequest? by mutableStateOf(null)

  override val status: TextToolbarStatus
    get() = if (request == null) TextToolbarStatus.Hidden else TextToolbarStatus.Shown

  override fun showMenu(
    rect: Rect,
    onCopyRequested: (() -> Unit)?,
    onPasteRequested: (() -> Unit)?,
    onCutRequested: (() -> Unit)?,
    onSelectAllRequested: (() -> Unit)?,
  ) {
    request =
      TextToolbarRequest(
        rect = rect,
        onCopyRequested = onCopyRequested,
        onPasteRequested = onPasteRequested,
        onCutRequested = onCutRequested,
        onSelectAllRequested = onSelectAllRequested,
        onAutofillRequested = null,
      )
  }

  override fun showMenu(
    rect: Rect,
    onCopyRequested: (() -> Unit)?,
    onPasteRequested: (() -> Unit)?,
    onCutRequested: (() -> Unit)?,
    onSelectAllRequested: (() -> Unit)?,
    onAutofillRequested: (() -> Unit)?,
  ) {
    request =
      TextToolbarRequest(
        rect = rect,
        onCopyRequested = onCopyRequested,
        onPasteRequested = onPasteRequested,
        onCutRequested = onCutRequested,
        onSelectAllRequested = onSelectAllRequested,
        onAutofillRequested = onAutofillRequested,
      )
  }

  override fun hide() {
    request = null
  }

  @Composable
  fun Content() {
    val currentRequest = request ?: return
    content(currentRequest.toState(), TextToolbarActions(currentRequest, ::hide))
  }
}

internal val defaultGlfwTextToolbarContent: GlfwTextToolbarContent = { state, actions ->
  Popup(
    popupPositionProvider = TextToolbarPositionProvider(state.rect),
    onDismissRequest = actions::dismiss,
    properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
  ) {
    Row(
      Modifier
        .background(TextToolbarBackground, RoundedCornerShape(6.dp))
        .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
      TextToolbarAction("Cut", state.canCut, actions::cut)
      TextToolbarAction("Copy", state.canCopy, actions::copy)
      TextToolbarAction("Paste", state.canPaste, actions::paste)
      TextToolbarAction("Select all", state.canSelectAll, actions::selectAll)
      TextToolbarAction("Autofill", state.canAutofill, actions::autofill)
    }
  }
}

@Composable
private fun TextToolbarAction(label: String, enabled: Boolean, action: () -> Unit) {
  if (!enabled) {
    return
  }
  BasicText(
    text = label,
    modifier =
      Modifier
        .clickable(onClick = action)
        .padding(horizontal = 10.dp, vertical = 7.dp),
    style = TextStyle(color = TextToolbarContent),
  )
}

private data class TextToolbarRequest(
  val rect: Rect,
  val onCopyRequested: (() -> Unit)?,
  val onPasteRequested: (() -> Unit)?,
  val onCutRequested: (() -> Unit)?,
  val onSelectAllRequested: (() -> Unit)?,
  val onAutofillRequested: (() -> Unit)?,
)

private fun TextToolbarRequest.toState(): GlfwTextToolbarState =
  GlfwTextToolbarState(
    rect = rect,
    canCopy = onCopyRequested != null,
    canPaste = onPasteRequested != null,
    canCut = onCutRequested != null,
    canSelectAll = onSelectAllRequested != null,
    canAutofill = onAutofillRequested != null,
  )

private class TextToolbarActions(
  private val request: TextToolbarRequest,
  private val hide: () -> Unit,
) : GlfwTextToolbarActions {
  override fun copy() {
    invokeAndHide(request.onCopyRequested)
  }

  override fun paste() {
    invokeAndHide(request.onPasteRequested)
  }

  override fun cut() {
    invokeAndHide(request.onCutRequested)
  }

  override fun selectAll() {
    invokeAndHide(request.onSelectAllRequested)
  }

  override fun autofill() {
    invokeAndHide(request.onAutofillRequested)
  }

  override fun dismiss() {
    hide()
  }

  private fun invokeAndHide(action: (() -> Unit)?) {
    if (action == null) {
      return
    }
    action()
    hide()
  }
}

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
