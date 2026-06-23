package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

public typealias GlfwTextToolbarContent = @Composable (
  state: GlfwTextToolbarState,
  actions: GlfwTextToolbarActions,
) -> Unit

public data class GlfwTextToolbarState(
  /** Selection bounds in the Compose scene's global coordinate space. */
  public val rect: Rect,
  public val canCopy: Boolean,
  public val canPaste: Boolean,
  public val canCut: Boolean,
  public val canSelectAll: Boolean,
  public val canAutofill: Boolean,
)

public interface GlfwTextToolbarActions {
  public fun copy()

  public fun paste()

  public fun cut()

  public fun selectAll()

  public fun autofill()

  public fun dismiss()
}
