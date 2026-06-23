package dev.sargunv.composeglfw

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

/** Values and services available to content hosted in a GLFW window. */
public interface HostWindowScope {
  /** Current host window and rendering information. */
  public val windowInfo: HostWindowInfo

  /** Backend-specific GPU interop for the current window. */
  public val gpu: GpuInterop
}

/** Current host window, display, and renderer information. */
public data class HostWindowInfo(
  /** Display server used by GLFW for this window. */
  public val displayServer: DisplayServer,

  /** Name of the display or monitor when GLFW reports one. */
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
