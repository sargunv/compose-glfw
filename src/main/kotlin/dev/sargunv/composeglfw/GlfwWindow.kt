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
  public val framebufferWidth: Int,
  public val framebufferHeight: Int,
  public val windowWidth: Int,
  public val windowHeight: Int,
  public val contentScale: Float,
)
