package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.fileDropTarget
import java.nio.file.Path

@Composable
internal fun FileDropCard(modifier: Modifier = Modifier) {
  var active by remember { mutableStateOf(false) }
  val droppedFiles = remember { mutableStateListOf<Path>() }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("File drop", style = MaterialTheme.typography.titleMedium)
      Column(
        Modifier
          .fillMaxWidth()
          .heightIn(min = 112.dp)
          .fileDropTarget(
            onEntered = { active = true },
            onEnded = { active = false },
            onDrop = { files ->
              droppedFiles.clear()
              droppedFiles.addAll(files.paths)
              active = false
            },
          )
          .border(
            width = 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          )
          .background(
            if (active) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest
            },
          )
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (droppedFiles.isEmpty()) {
          Text("Drop files here", style = MaterialTheme.typography.bodyMedium)
        } else {
          droppedFiles.take(4).forEach { path ->
            Text(path.fileName?.toString() ?: path.toString(), style = MaterialTheme.typography.bodyMedium)
          }
          if (droppedFiles.size > 4) {
            Text("+${droppedFiles.size - 4} more", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}
