package dev.sargunv.composeglfw.internal.render.opengl

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.OpenGlInterop
import dev.sargunv.composeglfw.RenderBackend
import dev.sargunv.composeglfw.internal.render.RenderBackendDriver
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import java.lang.invoke.MethodHandles
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.GLAssembledInterface
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skia.makeGLWithInterface
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwGetProcAddress
import org.lwjgl.glfw.GLFW.nglfwGetProcAddress
import org.lwjgl.glfw.GLFWNativeEGL.glfwGetEGLConfig
import org.lwjgl.glfw.GLFWNativeEGL.glfwGetEGLContext
import org.lwjgl.glfw.GLFWNativeEGL.glfwGetEGLDisplay
import org.lwjgl.opengl.GL11.GL_STENCIL_BITS
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL13.GL_SAMPLES
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING
import org.lwjgl.system.APIUtil.apiCreateCIF
import org.lwjgl.system.Callback
import org.lwjgl.system.CallbackI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memGetAddress
import org.lwjgl.system.MemoryUtil.memPutAddress
import org.lwjgl.system.Pointer.POINTER_SIZE
import org.lwjgl.system.libffi.LibFFI.ffi_type_pointer

internal class OpenGlRenderBackend(private val window: PlatformWindow) : RenderBackendDriver {
  private var glProcAddressCallback: GLProcAddressCallback? = null
  private var glInterface: GLAssembledInterface? = null
  private var skiaTarget: SkiaTarget? = null
  private val directContext: DirectContext

  override val backend: RenderBackend = RenderBackend.OPENGL

  override val interop: OpenGlInterop

  init {
    window.makeCurrent()
    directContext = createDirectContext()
    skiaTarget = createSkiaTarget(window.framebufferSize)
    interop =
      OpenGlInterop(
        directContext = directContext,
        eglDisplay = glfwGetEGLDisplay(),
        eglConfig = getEglConfig(),
        eglContext = glfwGetEGLContext(window.handle),
        getProcAddress = GLFW.Functions.GetProcAddress,
        resolveProcAddress = { name -> glfwGetProcAddress(name) },
        makeCurrent = window::makeCurrent,
      )
  }

  override fun resize(size: IntSize) {
    val target = skiaTarget
    if (target != null && target.width == size.width && target.height == size.height) {
      return
    }
    skiaTarget?.close()
    skiaTarget = null
    if (size.width > 0 && size.height > 0) {
      window.makeCurrent()
      skiaTarget = createSkiaTarget(size)
    }
  }

  override fun render(scene: ComposeWindowScene, frameTimeNanos: Long) {
    draw(scene, frameTimeNanos)
    window.swapBuffers()
  }

  override fun renderWithoutPresenting(scene: ComposeWindowScene, frameTimeNanos: Long) {
    draw(scene, frameTimeNanos)
  }

  private fun draw(scene: ComposeWindowScene, frameTimeNanos: Long) {
    val target = skiaTarget ?: return
    window.makeCurrent()
    val clearColor = if (window.isTransparent) SkiaColor.TRANSPARENT else SkiaColor.BLACK
    target.surface.canvas.clear(clearColor)
    scene.render(target.surface.canvas.asComposeCanvas(), frameTimeNanos)
    target.surface.flushAndSubmit()
  }

  override fun close() {
    window.makeCurrent()
    skiaTarget?.close()
    skiaTarget = null
    directContext.close()
    glInterface?.close()
    glInterface = null
    glProcAddressCallback?.free()
    glProcAddressCallback = null
  }

  private fun createSkiaTarget(size: IntSize): SkiaTarget {
    val renderTarget =
      BackendRenderTarget.makeGL(
        size.width,
        size.height,
        glGetInteger(GL_SAMPLES),
        glGetInteger(GL_STENCIL_BITS),
        glGetInteger(GL_FRAMEBUFFER_BINDING),
        FramebufferFormat.GR_GL_RGBA8,
      )
    val surface =
      Surface.makeFromBackendRenderTarget(
        directContext,
        renderTarget,
        SurfaceOrigin.BOTTOM_LEFT,
        SurfaceColorFormat.RGBA_8888,
        ColorSpace.sRGB,
        SurfaceProps(),
      )
    return SkiaTarget(size.width, size.height, renderTarget, checkNotNull(surface))
  }

  private fun createDirectContext(): DirectContext =
    try {
      DirectContext.makeGL()
    } catch (_: RuntimeException) {
      val getProcAddress =
        object : GLProcAddressCallback() {
          override fun invoke(ctx: Long, name: Long): Long = nglfwGetProcAddress(name)
        }
      val assembledInterface =
        GLAssembledInterface.createFromNativePointers(NULL, getProcAddress.address())
      glProcAddressCallback = getProcAddress
      glInterface = assembledInterface
      DirectContext.makeGLWithInterface(assembledInterface)
    }

  private fun getEglConfig(): Long =
    MemoryStack.stackPush().use { stack ->
      val config = stack.mallocPointer(1)
      check(glfwGetEGLConfig(window.handle, config)) {
        "GLFW did not expose the EGLConfig for this window"
      }
      config[0]
    }

  private data class SkiaTarget(
    val width: Int,
    val height: Int,
    val renderTarget: BackendRenderTarget,
    val surface: Surface,
  ) : AutoCloseable {
    override fun close() {
      surface.close()
      renderTarget.close()
    }
  }
}

private abstract class GLProcAddressCallback :
  Callback(GLProcAddressCallbackI.DESCRIPTOR), GLProcAddressCallbackI {
  override fun address(): Long = super<Callback>.address()
}

@FunctionalInterface
private fun interface GLProcAddressCallbackI : CallbackI {
  companion object {
    val DESCRIPTOR =
      Callback.Descriptor(
        MethodHandles.lookup(),
        apiCreateCIF(ffi_type_pointer, ffi_type_pointer, ffi_type_pointer),
      )
  }

  override fun getDescriptor(): Callback.Descriptor = DESCRIPTOR

  override fun callback(ret: Long, args: Long) {
    memPutAddress(
      ret,
      invoke(memGetAddress(args), memGetAddress(memGetAddress(args + POINTER_SIZE))),
    )
  }

  fun invoke(ctx: Long, name: Long): Long
}
