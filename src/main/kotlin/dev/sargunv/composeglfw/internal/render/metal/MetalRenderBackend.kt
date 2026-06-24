package dev.sargunv.composeglfw.internal.render.metal

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.MetalInterop
import dev.sargunv.composeglfw.RenderBackend
import dev.sargunv.composeglfw.internal.platform.HostOperatingSystem
import dev.sargunv.composeglfw.internal.platform.hostOperatingSystem
import dev.sargunv.composeglfw.internal.platform.macos.MacObjectiveC
import dev.sargunv.composeglfw.internal.render.RenderBackendDriver
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaView
import org.lwjgl.system.MemoryUtil.NULL

internal class MetalRenderBackend(private val window: PlatformWindow) : RenderBackendDriver {
  private var view: Long = NULL
  private var device: Long = NULL
  private var queue: Long = NULL
  private var layer: Long = NULL
  private var currentSize: IntSize = window.framebufferSize
  private val directContext: DirectContext

  override val backend: RenderBackend = RenderBackend.METAL

  override val interop: MetalInterop

  init {
    check(hostOperatingSystem == HostOperatingSystem.MACOS) {
      "Metal rendering is only supported on macOS"
    }

    MacObjectiveC.autoreleasePool().use {
      view = requireNativePointer(glfwGetCocoaView(window.handle), "GLFW Cocoa NSView")
      view = MacObjectiveC.retain(view)
      device = requireNativePointer(MacObjectiveC.metalSystemDefaultDevice(), "MTLDevice")
      queue =
        requireNativePointer(
          MacObjectiveC.sendPointer(device, "newCommandQueue"),
          "MTLCommandQueue",
        )
      layer = requireNativePointer(MacObjectiveC.allocInit("CAMetalLayer"), "CAMetalLayer")
      configureLayer()
    }

    directContext = DirectContext.makeMetal(device, queue)
    interop =
      MetalInterop(
        directContext = directContext,
        view = view,
        layer = layer,
        device = device,
        queue = queue,
      )
  }

  override fun resize(size: IntSize) {
    currentSize = size
    if (size.width > 0 && size.height > 0) {
      updateDrawableSize(size)
    }
  }

  override fun render(scene: ComposeWindowScene, frameTimeNanos: Long) {
    draw(scene, frameTimeNanos)
  }

  override fun renderWithoutPresenting(scene: ComposeWindowScene, frameTimeNanos: Long) = Unit

  private fun draw(
    scene: ComposeWindowScene,
    frameTimeNanos: Long,
  ) {
    val size = currentSize
    if (size.width <= 0 || size.height <= 0) return

    MacObjectiveC.autoreleasePool().use {
      updateDrawableSize(size)
      val drawable = MacObjectiveC.sendPointer(layer, "nextDrawable")
      if (drawable == NULL) return
      val texture = MacObjectiveC.sendPointer(drawable, "texture")
      if (texture == NULL) return

      BackendRenderTarget.makeMetal(size.width, size.height, texture).use { renderTarget ->
        val surface =
          checkNotNull(
            Surface.makeFromBackendRenderTarget(
              directContext,
              renderTarget,
              SurfaceOrigin.TOP_LEFT,
              SurfaceColorFormat.BGRA_8888,
              ColorSpace.sRGB,
              SurfaceProps(),
            )
          )
        surface.use {
          renderToSurface(surface, scene, frameTimeNanos)
        }
      }

      val commandBuffer = MacObjectiveC.sendPointer(queue, "commandBuffer")
      if (commandBuffer != NULL) {
        MacObjectiveC.sendVoid(commandBuffer, "presentDrawable:", drawable)
        MacObjectiveC.sendVoid(commandBuffer, "commit")
      }
    }
  }

  private fun renderToSurface(
    surface: Surface,
    scene: ComposeWindowScene,
    frameTimeNanos: Long,
  ) {
    val clearColor = if (window.isTransparent) SkiaColor.TRANSPARENT else SkiaColor.BLACK
    surface.canvas.clear(clearColor)
    scene.render(surface.canvas.asComposeCanvas(), frameTimeNanos)
    surface.flushAndSubmit()
  }

  override fun close() {
    directContext.close()
    MacObjectiveC.autoreleasePool().use {
      if (view != NULL && layer != NULL) {
        MacObjectiveC.sendVoid(view, "setLayer:", NULL)
      }
      MacObjectiveC.release(layer)
      MacObjectiveC.release(queue)
      MacObjectiveC.release(device)
      MacObjectiveC.release(view)
    }
    layer = NULL
    queue = NULL
    device = NULL
    view = NULL
  }

  private fun configureLayer() {
    MacObjectiveC.sendVoid(layer, "setDevice:", device)
    MacObjectiveC.sendVoid(layer, "setPixelFormat:", MtlPixelFormatBgra8Unorm)
    MacObjectiveC.sendVoid(layer, "setFramebufferOnly:", false)
    MacObjectiveC.sendVoid(layer, "setOpaque:", !window.isTransparent)
    updateDrawableSize(currentSize)
    MacObjectiveC.sendVoid(view, "setWantsLayer:", true)
    MacObjectiveC.sendVoid(view, "setLayer:", layer)
  }

  private fun updateDrawableSize(size: IntSize) {
    MacObjectiveC.sendDouble(layer, "setContentsScale:", window.contentScale.toDouble())
    MacObjectiveC.sendSize(layer, "setDrawableSize:", size.width.toDouble(), size.height.toDouble())
  }
}

private const val MtlPixelFormatBgra8Unorm: Int = 80

private fun requireNativePointer(
  pointer: Long,
  name: String,
): Long {
  check(pointer != NULL) { "$name is null" }
  return pointer
}
