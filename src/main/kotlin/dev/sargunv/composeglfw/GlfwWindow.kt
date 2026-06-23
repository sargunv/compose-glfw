package dev.sargunv.composeglfw

public data class GlfwWindowSize(public val width: Int, public val height: Int) {
  init {
    require(width > 0) { "Window width must be positive" }
    require(height > 0) { "Window height must be positive" }
  }
}

public data class GlfwWindowOptions(public val resizable: Boolean = true)

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
