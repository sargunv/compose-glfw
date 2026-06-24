package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun DialogCard(modifier: Modifier = Modifier) {
  var dialog by remember { mutableStateOf<DemoDialog?>(null) }
  var result by remember { mutableStateOf("No dialog action selected") }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Dialogs and modals", style = MaterialTheme.typography.titleMedium)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(onClick = { dialog = DemoDialog.Alert }) {
          Text("Alert")
        }
        Button(onClick = { dialog = DemoDialog.Form }) {
          Text("Form")
        }
        OutlinedButton(onClick = { dialog = DemoDialog.Modal }) {
          Text("Modal confirm")
        }
      }

      Text(
        text = result,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }

  when (dialog) {
    DemoDialog.Alert ->
      AlertDialog(
        onDismissRequest = {
          result = "Alert dismissed"
          dialog = null
        },
        title = { Text("Reset demo state") },
        text = { Text("This is a Compose dialog rendered as a scene layer in the GLFW window.") },
        confirmButton = {
          TextButton(
            onClick = {
              result = "Alert confirmed"
              dialog = null
            }
          ) {
            Text("Reset")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              result = "Alert canceled"
              dialog = null
            }
          ) {
            Text("Cancel")
          }
        },
      )
    DemoDialog.Form ->
      FormDialog(
        onSubmit = { value ->
          result = "Form submitted: $value"
          dialog = null
        },
        onDismiss = {
          result = "Form dismissed"
          dialog = null
        },
      )
    DemoDialog.Modal ->
      AlertDialog(
        onDismissRequest = {
          result = "Modal close requested"
        },
        title = { Text("Modal confirmation") },
        text = { Text("Outside clicks are ignored; choose an action to close this dialog.") },
        confirmButton = {
          Button(
            onClick = {
              result = "Modal confirmed"
              dialog = null
            }
          ) {
            Text("Confirm")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              result = "Modal canceled"
              dialog = null
            }
          ) {
            Text("Cancel")
          }
        },
        properties =
          DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
          ),
      )
    null -> Unit
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDialog(
  onSubmit: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var text by remember { mutableStateOf("Compose GLFW") }

  BasicAlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnClickOutside = true),
  ) {
    Surface(
      modifier = Modifier.widthIn(min = 280.dp, max = 420.dp),
      shape = MaterialTheme.shapes.extraLarge,
      tonalElevation = 6.dp,
    ) {
      Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Edit label", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Label") },
          singleLine = true,
        )
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Button(onClick = { onSubmit(text) }) {
            Text("Save")
          }
        }
      }
    }
  }
}

private enum class DemoDialog {
  Alert,
  Form,
  Modal,
}
