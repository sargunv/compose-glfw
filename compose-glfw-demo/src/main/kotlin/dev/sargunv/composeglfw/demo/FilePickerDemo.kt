package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.FileDialogFilter
import dev.sargunv.composeglfw.LocalWindow
import java.nio.file.Path

@Composable
internal fun FilePickerCard(modifier: Modifier = Modifier) {
  val filePicker = LocalWindow.current.filePicker
  var result by remember { mutableStateOf("No file picker action selected") }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("File picker", style = MaterialTheme.typography.titleMedium)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = {
            result = pickerResult {
              filePicker.openFile(filters = ImageFilters)?.formatPath() ?: "Open canceled"
            }
          }
        ) {
          Text("Open image")
        }

        Button(
          onClick = {
            result = pickerResult {
              filePicker.openFiles(filters = SourceFilters).formatPaths("No files selected")
            }
          }
        ) {
          Text("Open files")
        }

        Button(
          onClick = {
            result = pickerResult {
              filePicker.saveFile(defaultName = "untitled.txt", filters = TextFilters)?.formatPath()
                ?: "Save canceled"
            }
          }
        ) {
          Text("Save text")
        }

        Button(
          onClick = {
            result = pickerResult {
              filePicker.pickFolder()?.formatPath() ?: "Folder picker canceled"
            }
          }
        ) {
          Text("Pick folder")
        }

        Button(
          onClick = {
            result = pickerResult {
              filePicker.pickFolders().formatPaths("No folders selected")
            }
          }
        ) {
          Text("Pick folders")
        }
      }

      Text(
        text = result,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private inline fun pickerResult(action: () -> String): String =
  runCatching(action).getOrElse { throwable -> "Picker failed: ${throwable.message}" }

private fun Path.formatPath(): String = toString()

private fun List<Path>.formatPaths(emptyMessage: String): String =
  if (isEmpty()) {
    emptyMessage
  } else {
    joinToString(separator = "\n", limit = 4, truncated = "+${size - 4} more") { it.formatPath() }
  }

private val ImageFilters =
  listOf(FileDialogFilter("Images", listOf("png", "jpg", "jpeg", "gif", "webp")))

private val SourceFilters =
  listOf(
    FileDialogFilter("Kotlin", listOf("kt", "kts")),
    FileDialogFilter("Text", listOf("txt", "md")),
  )

private val TextFilters = listOf(FileDialogFilter("Text", listOf("txt", "md")))
