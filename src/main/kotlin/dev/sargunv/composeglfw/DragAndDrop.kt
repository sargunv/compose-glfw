@file:OptIn(ExperimentalComposeUiApi::class)

package dev.sargunv.composeglfw

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTargetModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import java.nio.file.Path

/** Files dropped onto a Compose GLFW window. */
public data class DroppedFiles(
  /** Paths reported by GLFW for the current file drag or drop. */
  public val paths: List<Path>
)

/**
 * Adds a file drop target to this modifier chain.
 *
 * All callbacks receive the paths currently being dragged or dropped. The target only participates
 * when [accept] returns true.
 *
 * @param accept returns true when this target should handle the dragged files.
 * @param onStarted called when an accepted file drag starts.
 * @param onEntered called when an accepted file drag enters this target.
 * @param onMoved called when an accepted file drag moves over this target.
 * @param onExited called when an accepted file drag exits this target.
 * @param onChanged called when an accepted file drag changes.
 * @param onEnded called when an accepted file drag ends.
 * @param onDrop called when files are dropped on this target.
 */
public fun Modifier.fileDropTarget(
  accept: (DroppedFiles) -> Boolean = { true },
  onStarted: (DroppedFiles) -> Unit = {},
  onEntered: (DroppedFiles) -> Unit = {},
  onMoved: (DroppedFiles) -> Unit = {},
  onExited: (DroppedFiles) -> Unit = {},
  onChanged: (DroppedFiles) -> Unit = {},
  onEnded: (DroppedFiles) -> Unit = {},
  onDrop: (DroppedFiles) -> Unit,
): Modifier =
  this then
    FileDropTargetElement(
      accept = accept,
      onStarted = onStarted,
      onEntered = onEntered,
      onMoved = onMoved,
      onExited = onExited,
      onChanged = onChanged,
      onEnded = onEnded,
      onDrop = onDrop,
    )

/** Returns the dropped files carried by this event, or null if it is not a file drop event. */
public fun DragAndDropEvent.droppedFilesOrNull(): DroppedFiles? = nativeEvent as? DroppedFiles

private data class FileDropTargetElement(
  val accept: (DroppedFiles) -> Boolean,
  val onStarted: (DroppedFiles) -> Unit,
  val onEntered: (DroppedFiles) -> Unit,
  val onMoved: (DroppedFiles) -> Unit,
  val onExited: (DroppedFiles) -> Unit,
  val onChanged: (DroppedFiles) -> Unit,
  val onEnded: (DroppedFiles) -> Unit,
  val onDrop: (DroppedFiles) -> Unit,
) : ModifierNodeElement<FileDropTargetNode>() {
  override fun create(): FileDropTargetNode =
    FileDropTargetNode(
      accept = accept,
      onStarted = onStarted,
      onEntered = onEntered,
      onMoved = onMoved,
      onExited = onExited,
      onChanged = onChanged,
      onEnded = onEnded,
      onDrop = onDrop,
    )

  override fun update(node: FileDropTargetNode) {
    node.update(
      accept = accept,
      onStarted = onStarted,
      onEntered = onEntered,
      onMoved = onMoved,
      onExited = onExited,
      onChanged = onChanged,
      onEnded = onEnded,
      onDrop = onDrop,
    )
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "fileDropTarget"
  }
}

private class FileDropTargetNode(
  private var accept: (DroppedFiles) -> Boolean,
  private var onStarted: (DroppedFiles) -> Unit,
  private var onEntered: (DroppedFiles) -> Unit,
  private var onMoved: (DroppedFiles) -> Unit,
  private var onExited: (DroppedFiles) -> Unit,
  private var onChanged: (DroppedFiles) -> Unit,
  private var onEnded: (DroppedFiles) -> Unit,
  private var onDrop: (DroppedFiles) -> Unit,
) : DelegatingNode(), DragAndDropTarget {
  init {
    delegate(
      DragAndDropTargetModifierNode(
        shouldStartDragAndDrop = { event -> event.droppedFilesOrNull()?.let(accept) == true },
        target = this,
      )
    )
  }

  fun update(
    accept: (DroppedFiles) -> Boolean,
    onStarted: (DroppedFiles) -> Unit,
    onEntered: (DroppedFiles) -> Unit,
    onMoved: (DroppedFiles) -> Unit,
    onExited: (DroppedFiles) -> Unit,
    onChanged: (DroppedFiles) -> Unit,
    onEnded: (DroppedFiles) -> Unit,
    onDrop: (DroppedFiles) -> Unit,
  ) {
    this.accept = accept
    this.onStarted = onStarted
    this.onEntered = onEntered
    this.onMoved = onMoved
    this.onExited = onExited
    this.onChanged = onChanged
    this.onEnded = onEnded
    this.onDrop = onDrop
  }

  override fun onStarted(event: DragAndDropEvent) {
    event.withDroppedFiles(onStarted)
  }

  override fun onEntered(event: DragAndDropEvent) {
    event.withDroppedFiles(onEntered)
  }

  override fun onMoved(event: DragAndDropEvent) {
    event.withDroppedFiles(onMoved)
  }

  override fun onExited(event: DragAndDropEvent) {
    event.withDroppedFiles(onExited)
  }

  override fun onChanged(event: DragAndDropEvent) {
    event.withDroppedFiles(onChanged)
  }

  override fun onEnded(event: DragAndDropEvent) {
    event.withDroppedFiles(onEnded)
  }

  override fun onDrop(event: DragAndDropEvent): Boolean {
    val droppedFiles = event.droppedFilesOrNull() ?: return false
    onDrop(droppedFiles)
    return true
  }
}

private inline fun DragAndDropEvent.withDroppedFiles(action: (DroppedFiles) -> Unit) {
  droppedFilesOrNull()?.let(action)
}
