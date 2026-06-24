package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun PopupMenuCard(modifier: Modifier = Modifier) {
  val buttons = listOf("Button A", "Button B", "Button C", "Button D", "Button E", "Button F")
  var contextAction by remember { mutableStateOf("No context menu action selected") }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Popup menus", style = MaterialTheme.typography.titleMedium)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        buttons.forEachIndexed { index, name ->
          TooltipArea(
            tooltip = {
              Surface(
                color = Color(255, 255, 210),
                contentColor = Color.Black,
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 4.dp,
              ) {
                Text(
                  text = "Tooltip for $name",
                  modifier = Modifier.padding(10.dp),
                )
              }
            },
            delayMillis = 600,
            tooltipPlacement =
              TooltipPlacement.CursorPoint(
                alignment = Alignment.BottomEnd,
                offset =
                  DpOffset(
                    x = if (index % 2 == 0) (-16).dp else 0.dp,
                    y = 16.dp,
                  ),
              ),
          ) {
            Button(onClick = {}) {
              Text(name)
            }
          }
        }
      }

      ContextMenuArea(
        items = {
          listOf(
            ContextMenuItem("Mark context target") {
              contextAction = "Marked context target"
            },
            ContextMenuItem("Reset status") {
              contextAction = "No context menu action selected"
            },
            ContextMenuItem("Disabled action", enabled = false) {
              contextAction = "Disabled action selected"
            },
          )
        }
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(96.dp)
              .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp),
              )
              .padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Right-click for context menu", style = MaterialTheme.typography.bodyMedium)
            Text(
              contextAction,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        DropdownSelect(
          label = "Renderer preference",
          options = listOf("Automatic", "OpenGL", "Metal", "Direct3D"),
        )
        DropdownSelect(
          label = "Pointer mode",
          options = listOf("Default", "Precise", "Large target"),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelect(
  label: String,
  options: List<String>,
) {
  var expanded by remember { mutableStateOf(false) }
  var selected by remember(options) { mutableStateOf(options.first()) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = Modifier.widthIn(min = 220.dp),
  ) {
    OutlinedTextField(
      value = selected,
      onValueChange = {},
      readOnly = true,
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      singleLine = true,
    )

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            selected = option
            expanded = false
          },
        )
      }
    }
  }
}
