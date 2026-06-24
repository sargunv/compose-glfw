package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values do not recreate or mutate an already remembered state.
 */
@Composable
public fun rememberWindowState(
  placement: WindowPlacement = WindowPlacement.Floating,
  isMinimized: Boolean = false,
  position: WindowPosition = WindowPosition.PlatformDefault,
  size: DpSize = DpSize(800.dp, 600.dp),
): WindowState = remember {
  WindowState(
    placement = placement,
    isMinimized = isMinimized,
    position = position,
    size = size,
  )
}

/** Creates a state object that can be hoisted to control and observe window attributes. */
public fun WindowState(
  placement: WindowPlacement = WindowPlacement.Floating,
  isMinimized: Boolean = false,
  position: WindowPosition = WindowPosition.PlatformDefault,
  size: DpSize = DpSize(800.dp, 600.dp),
): WindowState = WindowStateImpl(placement, isMinimized, position, size)

/** Runtime state for a GLFW window. */
@Stable
public interface WindowState {
  /** How the window is placed on the screen. */
  public var placement: WindowPlacement

  /** Whether the window is minimized. */
  public var isMinimized: Boolean

  /** Current window position in screen coordinates, when supported by the display server. */
  public var position: WindowPosition

  /**
   * Current logical content-area size.
   *
   * If one or both dimensions are [Dp.Unspecified], the window measures its content and replaces
   * this with the resolved concrete size when the size is applied.
   */
  public var size: DpSize
}

/** Describes how a window is placed on the screen. */
public enum class WindowPlacement {
  /** Floating window with normal platform decoration behavior. */
  Floating,

  /**
   * Maximized window, excluding platform-reserved screen areas when the display server supports it.
   */
  Maximized,

  /** Fullscreen window using the display server's native fullscreen behavior. */
  Fullscreen,
}

/** Position of a window on screen in [Dp]. */
@Immutable
public sealed class WindowPosition {
  /** Horizontal position. */
  @Stable public abstract val x: Dp

  /** Vertical position. */
  @Stable public abstract val y: Dp

  /** Whether this position has explicit coordinates. */
  @Stable public abstract val isSpecified: Boolean

  /** Let the platform choose the initial position. */
  public data object PlatformDefault : WindowPosition() {
    override val x: Dp = Dp.Unspecified
    override val y: Dp = Dp.Unspecified
    override val isSpecified: Boolean = false
  }

  /** Align the initial window position on the current screen. */
  public data class Aligned(public val alignment: Alignment) : WindowPosition() {
    override val x: Dp = Dp.Unspecified
    override val y: Dp = Dp.Unspecified
    override val isSpecified: Boolean = false
  }

  /** Absolute screen position. */
  public data class Absolute(
    override val x: Dp,
    override val y: Dp,
  ) : WindowPosition() {
    override val isSpecified: Boolean = true
  }
}

/** Constructs an absolute [WindowPosition] from [x] and [y]. */
public fun WindowPosition(
  x: Dp,
  y: Dp,
): WindowPosition = WindowPosition.Absolute(x, y)

/** Constructs an aligned [WindowPosition]. */
public fun WindowPosition(alignment: Alignment): WindowPosition = WindowPosition.Aligned(alignment)

private class WindowStateImpl(
  placement: WindowPlacement,
  isMinimized: Boolean,
  position: WindowPosition,
  size: DpSize,
) : WindowState {
  override var placement: WindowPlacement by mutableStateOf(placement)
  override var isMinimized: Boolean by mutableStateOf(isMinimized)
  override var position: WindowPosition by mutableStateOf(position)
  override var size: DpSize by mutableStateOf(size)
}
