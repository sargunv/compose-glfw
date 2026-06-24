package dev.sargunv.composeglfw

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import dev.sargunv.composeglfw.internal.platform.defaultTextToolbarContent

/**
 * Host-specific options used when creating a GLFW window.
 *
 * @property textToolbar content used for the text editing toolbar.
 */
public class WindowOptions private constructor(public val textToolbar: TextToolbarContent) {
  /** Factory for [WindowOptions]. */
  public companion object {
    /** Creates window options using the builder DSL. */
    public operator fun invoke(configure: Builder.() -> Unit = {}): WindowOptions =
      Builder().apply(configure).build()
  }

  /** Builder for [WindowOptions]. */
  public class Builder {
    /** Content used for the text editing toolbar. */
    public var textToolbar: TextToolbarContent = defaultTextToolbarContent

    /** Builds immutable window options from the current builder values. */
    public fun build(): WindowOptions = WindowOptions(textToolbar = textToolbar)
  }
}

/** Values available to content hosted in a GLFW window. */
public interface HostWindowScope {
  /** Current host window. */
  public val window: HostWindow
}

/** Current host window. */
public interface HostWindow {
  /** Current host window information. */
  public val info: HostWindowInfo

  /** Backend-specific render context for the current window. */
  public val renderContext: RenderContext

  /** Native file and folder picker associated with the current window. */
  public val filePicker: FilePicker
}

/** Current Compose GLFW window. */
public val LocalWindow: ProvidableCompositionLocal<HostWindow> = staticCompositionLocalOf {
  error("LocalWindow is only available inside a Compose GLFW Window")
}

/** Current host window, display, and renderer information. */
public data class HostWindowInfo(
  /** Display server used by GLFW for this window. */
  public val displayServer: DisplayServer,

  /** Platform display connection name, such as `WAYLAND_DISPLAY` or `DISPLAY`, when known. */
  public val displayName: String?,

  /** Renderer used to draw Compose content. */
  public val renderBackend: RenderBackend,

  /** Physical drawable pixels backing the Compose scene and Skia render target. */
  public val framebufferWidth: Int,

  /** Physical drawable pixels backing the Compose scene and Skia render target. */
  public val framebufferHeight: Int,

  /** Logical content-area width after applying host display scaling. */
  public val windowWidth: Int,

  /** Logical content-area height after applying host display scaling. */
  public val windowHeight: Int,

  /** GLFW content scale used as the Compose density. */
  public val contentScale: Float,
)
