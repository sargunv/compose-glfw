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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

@Composable
internal fun PointerIconCard(modifier: Modifier = Modifier) {
  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Pointer icons", style = MaterialTheme.typography.titleMedium)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PointerIconTarget("Default", PointerIcon.Default)
        PointerIconTarget("Hand", PointerIcon.Hand)
        PointerIconTarget("Text", PointerIcon.Text)
        PointerIconTarget("Crosshair", PointerIcon.Crosshair)
      }
    }
  }
}

@Composable
private fun PointerIconTarget(label: String, pointerIcon: PointerIcon) {
  Column(
    Modifier
      .width(132.dp)
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
