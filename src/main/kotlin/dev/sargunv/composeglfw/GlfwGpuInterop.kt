package dev.sargunv.composeglfw

import org.jetbrains.skia.DirectContext

public sealed interface GlfwGpuInterop {
  public val backend: GlfwRenderBackend
}

public data class GlfwOpenGlInterop(
  public val directContext: DirectContext,
  public val eglDisplay: Long,
  public val eglConfig: Long,
  public val eglContext: Long,
  public val getProcAddress: Long,
  public val resolveProcAddress: (String) -> Long,
  public val makeCurrent: () -> Unit,
) : GlfwGpuInterop {
  override val backend: GlfwRenderBackend = GlfwRenderBackend.OPENGL
}
