package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MaterialControlsCard(modifier: Modifier = Modifier) {
  var clicks by remember { mutableIntStateOf(0) }
  var enabled by remember { mutableStateOf(true) }
  var checked by remember { mutableStateOf(false) }
  var radioSelection by remember { mutableStateOf("Wayland") }
  var sliderValue by remember { mutableFloatStateOf(0.4f) }
  var text by remember { mutableStateOf("") }
  var menuOpen by remember { mutableStateOf(false) }
  var menuSelection by remember { mutableStateOf("OpenGL") }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Material controls", style = MaterialTheme.typography.titleMedium)
        Text(
          "Controls exercise pointer input now and give keyboard/text input a visible target.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { clicks++ }) {
          Text("Clicked $clicks")
        }
        OutlinedButton(onClick = { clicks = 0 }) {
          Text("Reset")
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = checked, onCheckedChange = { checked = it })
          Text("Checkbox")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = enabled, onCheckedChange = { enabled = it })
          Text("Switch")
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Slider: ${(sliderValue * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Slider(value = sliderValue, onValueChange = { sliderValue = it })
      }

      Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("Wayland", "OpenGL").forEach { option ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = radioSelection == option, onClick = { radioSelection = option })
            Text(option)
          }
        }
      }

      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Text input") },
        singleLine = true,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { menuOpen = true }) {
          Text("Menu: $menuSelection")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          listOf("OpenGL", "Metal", "Direct3D").forEach { option ->
            DropdownMenuItem(
              text = { Text(option) },
              onClick = {
                menuSelection = option
                menuOpen = false
              },
            )
          }
        }
      }
    }
  }
}
