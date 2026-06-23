package dev.sargunv.composeglfw

import org.jetbrains.skia.DirectContext

public sealed interface GpuInterop {
  public val backend: RenderBackend
}

public data class OpenGlInterop(
  public val directContext: DirectContext,
  public val eglDisplay: Long,
  public val eglConfig: Long,
  public val eglContext: Long,
  public val getProcAddress: Long,
  public val resolveProcAddress: (String) -> Long,
  public val makeCurrent: () -> Unit,
) : GpuInterop {
  override val backend: RenderBackend = RenderBackend.OPENGL
}
