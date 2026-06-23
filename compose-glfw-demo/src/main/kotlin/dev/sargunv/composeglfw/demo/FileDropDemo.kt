@file:OptIn(ExperimentalComposeUiApi::class)

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTargetModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.DroppedFiles
import dev.sargunv.composeglfw.droppedFilesOrNull
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

@Composable
private fun Modifier.fileDropTarget(
  onEntered: () -> Unit,
  onEnded: () -> Unit,
  onDrop: (DroppedFiles) -> Unit,
): Modifier {
  val target =
    remember {
      object : DragAndDropTarget {
        override fun onEntered(event: DragAndDropEvent) {
          onEntered()
        }

        override fun onEnded(event: DragAndDropEvent) {
          onEnded()
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
          val droppedFiles = event.droppedFilesOrNull() ?: return false
          onDrop(droppedFiles)
          return true
        }
      }
    }
  return this then FileDropTargetElement(
    shouldStart = { event -> event.droppedFilesOrNull() != null },
    target = target,
  )
}

private data class FileDropTargetElement(
  val shouldStart: (DragAndDropEvent) -> Boolean,
  val target: DragAndDropTarget,
) : ModifierNodeElement<Modifier.Node>() {
  override fun create(): Modifier.Node =
    DragAndDropTargetModifierNode(shouldStart, target) as Modifier.Node

  override fun update(node: Modifier.Node) = Unit

  override fun InspectorInfo.inspectableProperties() {
    name = "fileDropTarget"
  }
}
