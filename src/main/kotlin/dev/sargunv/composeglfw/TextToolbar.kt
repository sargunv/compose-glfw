package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

/**
 * Composable content used to show text editing actions.
 *
 * The content is called while Compose requests a text toolbar. Use the state to choose visible
 * actions and call the actions object when the user selects one.
 */
public typealias TextToolbarContent =
  @Composable
  (
    state: TextToolbarState,
    actions: TextToolbarActions,
  ) -> Unit

/** State passed to custom text toolbar content. */
public data class TextToolbarState(
  /** Selection bounds in the Compose scene's global coordinate space. */
  public val rect: Rect,

  /** Whether copy is available for the current selection. */
  public val canCopy: Boolean,

  /** Whether paste is available at the current selection. */
  public val canPaste: Boolean,

  /** Whether cut is available for the current selection. */
  public val canCut: Boolean,

  /** Whether select all is available for the focused text field. */
  public val canSelectAll: Boolean,

  /** Whether autofill is available for the focused text field. */
  public val canAutofill: Boolean,
)

/** Actions that custom text toolbar content can perform. */
public interface TextToolbarActions {
  /** Copies the current selection and dismisses the toolbar. */
  public fun copy()

  /** Pastes from the clipboard and dismisses the toolbar. */
  public fun paste()

  /** Cuts the current selection and dismisses the toolbar. */
  public fun cut()

  /** Selects all text in the focused text field and dismisses the toolbar. */
  public fun selectAll()

  /** Runs the current autofill action and dismisses the toolbar. */
  public fun autofill()

  /** Dismisses the toolbar without running an editing action. */
  public fun dismiss()
}
