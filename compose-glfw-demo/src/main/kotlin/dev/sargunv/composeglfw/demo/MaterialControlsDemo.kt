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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
internal fun MaterialControlsCard(modifier: Modifier = Modifier) {
  val model = viewModel<MaterialControlsViewModel>(factory = MaterialControlsViewModel.Factory)
  val textFocusRequester = remember { FocusRequester() }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Material controls", style = MaterialTheme.typography.titleMedium)

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = model::incrementClicks) {
          Text("Clicked ${model.clicks}")
        }
        OutlinedButton(onClick = model::resetClicks) {
          Text("Reset")
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = model.checked, onCheckedChange = model::updateChecked)
          Text("Checkbox")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = model.enabled, onCheckedChange = model::updateEnabled)
          Text("Switch")
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Slider: ${(model.sliderValue * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Slider(value = model.sliderValue, onValueChange = model::updateSliderValue)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("Small", "Large").forEach { option ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = model.radioSelection == option, onClick = { model.updateRadioSelection(option) })
            Text(option)
          }
        }
      }

      OutlinedTextField(
        value = model.text,
        onValueChange = model::updateText,
        modifier = Modifier.fillMaxWidth().focusRequester(textFocusRequester),
        label = { Text("Text input") },
        singleLine = true,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = model.password,
          onValueChange = model::updatePassword,
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
          selection = model.menuSelection,
          alignment = Alignment.CenterStart,
          onSelectionChange = model::updateMenuSelection,
        )
        MenuAnchor(
          label = "Center",
          selection = model.menuSelection,
          alignment = Alignment.Center,
          onSelectionChange = model::updateMenuSelection,
        )
        MenuAnchor(
          label = "End",
          selection = model.menuSelection,
          alignment = Alignment.CenterEnd,
          onSelectionChange = model::updateMenuSelection,
        )
      }
    }
  }
}

internal class MaterialControlsViewModel : ViewModel() {
  var clicks: Int by mutableIntStateOf(0)
    private set
  var enabled: Boolean by mutableStateOf(true)
    private set
  var checked: Boolean by mutableStateOf(false)
    private set
  var radioSelection: String by mutableStateOf("Small")
    private set
  var sliderValue: Float by mutableFloatStateOf(0.4f)
    private set
  var text: String by mutableStateOf("")
    private set
  var password: String by mutableStateOf("")
    private set
  var menuSelection: String by mutableStateOf("Daily")
    private set

  fun incrementClicks(): Unit {
    clicks++
  }

  fun resetClicks(): Unit {
    clicks = 0
  }

  fun updateEnabled(value: Boolean): Unit {
    enabled = value
  }

  fun updateChecked(value: Boolean): Unit {
    checked = value
  }

  fun updateRadioSelection(value: String): Unit {
    radioSelection = value
  }

  fun updateSliderValue(value: Float): Unit {
    sliderValue = value
  }

  fun updateText(value: String): Unit {
    text = value
  }

  fun updatePassword(value: String): Unit {
    password = value
  }

  fun updateMenuSelection(value: String): Unit {
    menuSelection = value
  }

  internal companion object {
    internal val Factory: ViewModelProvider.Factory =
      viewModelFactory {
        initializer {
          MaterialControlsViewModel()
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
    Box {
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
}
