package dev.sargunv.composeglfw

import dev.sargunv.composeglfw.internal.platform.defaultTextToolbarContent

public class WindowOptions private constructor(
  public val resizable: Boolean,
  public val transparentFramebuffer: Boolean,
  public val textToolbar: TextToolbarContent,
) {
  public companion object {
    public operator fun invoke(configure: Builder.() -> Unit = {}): WindowOptions =
      Builder().apply(configure).build()
  }

  public class Builder {
    public var resizable: Boolean = true
    public var transparentFramebuffer: Boolean = false
    public var textToolbar: TextToolbarContent = defaultTextToolbarContent

    public fun build(): WindowOptions =
      WindowOptions(
        resizable = resizable,
        transparentFramebuffer = transparentFramebuffer,
        textToolbar = textToolbar,
      )
  }
}

public interface HostWindowScope {
  public val windowInfo: HostWindowInfo

  public val gpu: GpuInterop
}

public data class HostWindowInfo(
  public val displayServer: DisplayServer,
  public val displayName: String?,
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
