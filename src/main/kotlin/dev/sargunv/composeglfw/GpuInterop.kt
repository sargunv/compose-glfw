package dev.sargunv.composeglfw

import org.jetbrains.skia.DirectContext

/** Backend-specific GPU interop for a hosted Compose window. */
public sealed interface GpuInterop {
  /** Rendering backend used by this interop object. */
  public val backend: RenderBackend
}

/** OpenGL interop for a Compose GLFW window. */
public data class OpenGlInterop(
  /** Skia direct context used by Compose for this window. */
  public val directContext: DirectContext,

  /** EGL display handle for the window context. */
  public val eglDisplay: Long,

  /** EGL config handle for the window context. */
  public val eglConfig: Long,

  /** EGL context handle used to render this window. */
  public val eglContext: Long,

  /** Raw GLFW `glfwGetProcAddress` function pointer. */
  public val getProcAddress: Long,

  /** Resolves an OpenGL procedure name to a function pointer. */
  public val resolveProcAddress: (String) -> Long,

  /** Makes this window's OpenGL context current on the calling thread. */
  public val makeCurrent: () -> Unit,
) : GpuInterop {
  override val backend: RenderBackend = RenderBackend.OPENGL
}
