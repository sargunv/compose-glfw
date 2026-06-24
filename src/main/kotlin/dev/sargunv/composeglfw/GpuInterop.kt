package dev.sargunv.composeglfw

import org.jetbrains.skia.DirectContext

/** Backend-specific GPU interop for a hosted Compose window. */
public sealed interface GpuInterop

/**
 * OpenGL interop for a Compose GLFW window.
 *
 * Native handles are borrowed from the host window and remain valid only until that window is
 * closed or recreated. Do not close or release them from user code.
 */
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
) : GpuInterop

/**
 * Metal interop for a Compose GLFW window.
 *
 * Native handles are borrowed from the host window and remain valid only until that window is
 * closed or recreated. Do not close or release them from user code. Cocoa and Metal objects should
 * be used from the application/UI thread unless their platform documentation says otherwise.
 */
public data class MetalInterop(
  /** Skia direct context used by Compose for this window. */
  public val directContext: DirectContext,

  /** Cocoa `NSView*` backing the GLFW window content. */
  public val view: Long,

  /** `CAMetalLayer*` used as the window drawable. */
  public val layer: Long,

  /** `MTLDevice*` used by Skia. */
  public val device: Long,

  /** `MTLCommandQueue*` used by Skia. */
  public val queue: Long,
) : GpuInterop
