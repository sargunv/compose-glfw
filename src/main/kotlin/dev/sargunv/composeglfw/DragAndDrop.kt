@file:OptIn(ExperimentalComposeUiApi::class)

package dev.sargunv.composeglfw

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import java.nio.file.Path

public data class DroppedFiles(
  public val paths: List<Path>,
)

public fun DragAndDropEvent.droppedFilesOrNull(): DroppedFiles? =
  nativeEvent as? DroppedFiles
