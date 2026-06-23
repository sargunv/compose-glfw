package dev.sargunv.composeglfw

import dev.sargunv.composeglfw.internal.platform.defaultGlfwTextToolbarContent

public data class GlfwWindowSize(public val width: Int, public val height: Int) {
  init {
    require(width > 0) { "Window width must be positive" }
    require(height > 0) { "Window height must be positive" }
  }
}

public class GlfwWindowOptions private constructor(
  public val resizable: Boolean,
  public val transparentFramebuffer: Boolean,
  public val textToolbar: GlfwTextToolbarContent,
) {
  public companion object {
    public operator fun invoke(configure: Builder.() -> Unit = {}): GlfwWindowOptions =
      Builder().apply(configure).build()
  }

  public class Builder {
    public var resizable: Boolean = true
    public var transparentFramebuffer: Boolean = false
    public var textToolbar: GlfwTextToolbarContent = defaultGlfwTextToolbarContent

    public fun build(): GlfwWindowOptions =
      GlfwWindowOptions(
        resizable = resizable,
        transparentFramebuffer = transparentFramebuffer,
        textToolbar = textToolbar,
      )
  }
}

public interface GlfwWindowScope {
  public val windowInfo: GlfwWindowInfo

  public val gpu: GlfwGpuInterop
}

public data class GlfwWindowInfo(
  public val platform: GlfwPlatform,
  public val displayName: String?,
  public val renderBackend: GlfwRenderBackend,
  /** Physical drawable pixels backing the Compose scene and Skia render target. */
  public val framebufferWidth: Int,
  /** Physical drawable pixels backing the Compose scene and Skia render target. */
  public val framebufferHeight: Int,
  /** GLFW content-area width in screen coordinates. */
  public val windowWidth: Int,
  /** GLFW content-area height in screen coordinates. */
  public val windowHeight: Int,
  /** GLFW content scale used as the Compose density. */
  public val contentScale: Float,
)
