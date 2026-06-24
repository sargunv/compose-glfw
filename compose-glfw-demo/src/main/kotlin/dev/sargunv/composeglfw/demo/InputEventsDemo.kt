package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.PointerEvent
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
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp

@Composable
internal fun InputEventsCard(modifier: Modifier = Modifier) {
  val focusRequester = remember { FocusRequester() }
  val inputMode = LocalInputModeManager.current.inputMode
  var keyState by remember { mutableStateOf(ObservedModifiers()) }
  var lastKey by remember { mutableStateOf("none") }
  var lastPointer by remember { mutableStateOf("none") }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Input events", style = MaterialTheme.typography.titleMedium)

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
                lastPointer = event.describe()
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

      Text(
        "Last key: $lastKey (${keyState.format()})",
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        "Last pointer: $lastPointer",
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        "Input mode: $inputMode",
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
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

private fun PointerEvent.describe(): String {
  val change = changes.firstOrNull()
  val position =
    if (change == null) {
      "unknown"
    } else {
      "${change.position.x.toInt()}, ${change.position.y.toInt()}"
    }
  val pressed =
    if (changes.any { it.pressed }) {
      "pressed"
    } else {
      "released"
    }
  return "$type at $position, $pressed (${keyboardModifiers.toObservedModifiers().format()})"
}

private fun ObservedModifiers.format(): String {
  val active = buildList {
    if (ctrl) add("Ctrl")
    if (shift) add("Shift")
    if (alt) add("Alt")
    if (meta) add("Meta")
    if (altGraph) add("AltGraph")
    if (capsLock) add("Caps")
    if (numLock) add("Num")
    if (scrollLock) add("Scroll")
  }
  return active.joinToString().ifEmpty { "no modifiers" }
}
