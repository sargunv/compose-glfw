package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.cursorImagePointerIcon
import dev.sargunv.composeglfw.demo.generated.resources.Res
import dev.sargunv.composeglfw.demo.generated.resources.custom_cursor
import org.jetbrains.compose.resources.imageResource

@Composable
internal fun PointerIconCard(modifier: Modifier = Modifier) {
  val customCursorImage = imageResource(Res.drawable.custom_cursor)
  val customPointerIcon =
    remember(customCursorImage) {
      cursorImagePointerIcon(customCursorImage, IntOffset(12, 12))
    }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Pointer icons", style = MaterialTheme.typography.titleMedium)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        PointerIconTarget("Default", PointerIcon.Default)
        PointerIconTarget("Hand", PointerIcon.Hand)
        PointerIconTarget("Text", PointerIcon.Text)
        PointerIconTarget("Crosshair", PointerIcon.Crosshair)
        PointerIconTarget("Custom", customPointerIcon)
      }
    }
  }
}

@Composable
private fun PointerIconTarget(label: String, pointerIcon: PointerIcon) {
  Column(
    Modifier.width(132.dp)
      .height(72.dp)
      .background(MaterialTheme.colorScheme.surfaceContainerHighest)
      .pointerHoverIcon(pointerIcon)
      .padding(12.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
  }
}
