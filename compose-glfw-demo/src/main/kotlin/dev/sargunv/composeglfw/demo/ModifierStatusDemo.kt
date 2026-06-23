package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltGraphPressed as isPointerAltGraphPressed
import androidx.compose.ui.input.pointer.isAltPressed as isPointerAltPressed
import androidx.compose.ui.input.pointer.isCapsLockOn as isPointerCapsLockOn
import androidx.compose.ui.input.pointer.isCtrlPressed as isPointerCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed as isPointerMetaPressed
import androidx.compose.ui.input.pointer.isNumLockOn as isPointerNumLockOn
import androidx.compose.ui.input.pointer.isScrollLockOn as isPointerScrollLockOn
import androidx.compose.ui.input.pointer.isShiftPressed as isPointerShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
internal fun ModifierStatusCard(modifier: Modifier = Modifier) {
  val focusRequester = remember { FocusRequester() }
  var keyState by remember { mutableStateOf(ObservedModifiers()) }
  var pointerState by remember { mutableStateOf(ObservedModifiers()) }
  var lastKey by remember { mutableStateOf("none") }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Modifier status", style = MaterialTheme.typography.titleMedium)

      Box(
        Modifier.fillMaxWidth()
          .height(96.dp)
          .background(MaterialTheme.colorScheme.surfaceContainerHighest)
          .focusRequester(focusRequester)
          .focusable()
          .onPreviewKeyEvent { event ->
            keyState = event.toObservedModifiers()
            lastKey = "${event.key} ${event.type}"
            false
          }
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                pointerState = event.keyboardModifiers.toObservedModifiers()
                if (event.changes.any { it.pressed }) {
                  focusRequester.requestFocus()
                }
              }
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        Text("Focus target")
      }

      Text("Last key: $lastKey", style = MaterialTheme.typography.bodyMedium)
      ModifierStatus("Key event", keyState)
      ModifierStatus("Pointer event", pointerState)
    }
  }
}

@Composable
private fun ModifierStatus(label: String, state: ObservedModifiers) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ModifierBadge("Ctrl", state.ctrl)
      ModifierBadge("Shift", state.shift)
      ModifierBadge("Alt", state.alt)
      ModifierBadge("Meta", state.meta)
      ModifierBadge("AltGraph", state.altGraph)
      ModifierBadge("Caps", state.capsLock)
      ModifierBadge("Num", state.numLock)
      ModifierBadge("Scroll", state.scrollLock)
    }
  }
}

@Composable
private fun ModifierBadge(label: String, active: Boolean) {
  val colors = MaterialTheme.colorScheme
  Text(
    text = "$label ${if (active) "on" else "off"}",
    modifier =
      Modifier
        .widthIn(min = 72.dp)
        .background(if (active) colors.primaryContainer else colors.surfaceContainer)
        .padding(horizontal = 10.dp, vertical = 6.dp),
    style = MaterialTheme.typography.labelMedium,
    color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
  )
}

private data class ObservedModifiers(
  val ctrl: Boolean = false,
  val shift: Boolean = false,
  val alt: Boolean = false,
  val meta: Boolean = false,
  val altGraph: Boolean = false,
  val capsLock: Boolean = false,
  val numLock: Boolean = false,
  val scrollLock: Boolean = false,
)

private fun KeyEvent.toObservedModifiers(): ObservedModifiers =
  ObservedModifiers(
    ctrl = isCtrlPressed,
    shift = isShiftPressed,
    alt = isAltPressed,
    meta = isMetaPressed,
  )

private fun PointerKeyboardModifiers.toObservedModifiers(): ObservedModifiers =
  ObservedModifiers(
    ctrl = isPointerCtrlPressed,
    shift = isPointerShiftPressed,
    alt = isPointerAltPressed,
    meta = isPointerMetaPressed,
    altGraph = isPointerAltGraphPressed,
    capsLock = isPointerCapsLockOn,
    numLock = isPointerNumLockOn,
    scrollLock = isPointerScrollLockOn,
  )
