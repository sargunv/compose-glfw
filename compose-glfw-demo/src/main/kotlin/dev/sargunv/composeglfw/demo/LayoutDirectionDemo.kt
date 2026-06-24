package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
internal fun LayoutDirectionCard(modifier: Modifier = Modifier) {
  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Layout direction", style = MaterialTheme.typography.titleMedium)
      DirectionSample("Left-to-right", LayoutDirection.Ltr)
      DirectionSample("Right-to-left", LayoutDirection.Rtl)
    }
  }
}

@Composable
private fun DirectionSample(
  label: String,
  direction: LayoutDirection,
) {
  CompositionLocalProvider(LocalLayoutDirection provides direction) {
    val currentDirection = LocalLayoutDirection.current
    var text by remember(direction) { mutableStateOf("$label text field") }

    Surface(
      shape = MaterialTheme.shapes.small,
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
      Column(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("$label: $currentDirection", style = MaterialTheme.typography.labelLarge)
          Text("End edge", style = MaterialTheme.typography.bodyMedium)
        }

        Row(
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Leading")
          Text("Center")
          Text("Trailing")
        }

        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Editable text") },
          singleLine = true,
        )
      }
    }
  }
}
