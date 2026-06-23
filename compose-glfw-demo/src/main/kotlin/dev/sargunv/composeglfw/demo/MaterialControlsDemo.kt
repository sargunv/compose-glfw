package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun MaterialControlsCard(modifier: Modifier = Modifier) {
  var clicks by remember { mutableIntStateOf(0) }
  var enabled by remember { mutableStateOf(true) }
  var checked by remember { mutableStateOf(false) }
  var radioSelection by remember { mutableStateOf("Small") }
  var sliderValue by remember { mutableFloatStateOf(0.4f) }
  var text by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var menuSelection by remember { mutableStateOf("Daily") }
  val textFocusRequester = remember { FocusRequester() }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Material controls", style = MaterialTheme.typography.titleMedium)

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
        listOf("Small", "Large").forEach { option ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = radioSelection == option, onClick = { radioSelection = option })
            Text(option)
          }
        }
      }

      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth().focusRequester(textFocusRequester),
        label = { Text("Text input") },
        singleLine = true,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          modifier = Modifier.weight(1f),
          label = { Text("Password") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
        )
        OutlinedButton(onClick = { textFocusRequester.requestFocus() }) {
          Text("Focus text")
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuAnchor(
          label = "Start",
          selection = menuSelection,
          alignment = Alignment.CenterStart,
          onSelectionChange = { menuSelection = it },
        )
        MenuAnchor(
          label = "Center",
          selection = menuSelection,
          alignment = Alignment.Center,
          onSelectionChange = { menuSelection = it },
        )
        MenuAnchor(
          label = "End",
          selection = menuSelection,
          alignment = Alignment.CenterEnd,
          onSelectionChange = { menuSelection = it },
        )
      }
    }
  }
}

@Composable
private fun MenuAnchor(
  label: String,
  selection: String,
  alignment: Alignment,
  onSelectionChange: (String) -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }

  Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = alignment) {
    OutlinedButton(onClick = { menuOpen = true }) {
      Text("$label: $selection")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      listOf("Daily", "Weekly", "Monthly", "Custom").forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            onSelectionChange(option)
            menuOpen = false
          },
        )
      }
    }
  }
}
