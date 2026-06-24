package dev.sargunv.composeglfw.internal.render.direct3d

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.Direct3DRenderContext
import dev.sargunv.composeglfw.RenderBackend
import dev.sargunv.composeglfw.internal.platform.windows.Com
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
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window

internal class Direct3DRenderBackend(private val window: PlatformWindow) : RenderBackendDriver {
  private val deviceResources: Direct3DDeviceResources
  private val directContext: DirectContext
  private var targets: List<SkiaTarget> = emptyList()

  override val backend: RenderBackend = RenderBackend.DIRECT3D

  override val interop: Direct3DRenderContext

  init {
    val hwnd = glfwGetWin32Window(window.handle)
    check(hwnd != 0L) { "GLFW did not expose a Win32 HWND for this window" }
    deviceResources = Direct3DDeviceResources(hwnd)
    directContext =
      DirectContext.makeDirect3D(
        deviceResources.adapter,
        deviceResources.device,
        deviceResources.commandQueue,
      )
    deviceResources.createSwapChain(window.framebufferSize)
    targets = createSkiaTargets(window.framebufferSize)
    interop =
      Direct3DRenderContext(
        directContext = directContext,
        hwnd = hwnd,
        adapter = deviceResources.adapter,
        device = deviceResources.device,
        commandQueue = deviceResources.commandQueue,
        swapChain = deviceResources.swapChain,
        compositionDevice = deviceResources.compositionDevice,
        compositionTarget = deviceResources.compositionTarget,
        compositionVisual = deviceResources.compositionVisual,
      )
  }

  override fun resize(size: IntSize) {
    if (targets.firstOrNull()?.let { it.width == size.width && it.height == size.height } == true) {
      return
    }
    closeTargets(syncCpu = true)
    if (size.width > 0 && size.height > 0) {
      deviceResources.resizeSwapChain(size)
      targets = createSkiaTargets(size)
    }
  }

  override fun render(scene: ComposeWindowScene, frameTimeNanos: Long) {
    draw(scene, frameTimeNanos)
    deviceResources.present()
  }

  private fun draw(scene: ComposeWindowScene, frameTimeNanos: Long) {
    val target = currentTarget() ?: return
    val clearColor = if (window.isTransparent) SkiaColor.TRANSPARENT else SkiaColor.BLACK
    target.surface.canvas.clear(clearColor)
    scene.render(target.surface.canvas.asComposeCanvas(), frameTimeNanos)
    target.surface.flushAndSubmit()
  }

  private fun currentTarget(): SkiaTarget? {
    if (targets.isEmpty()) {
      return null
    }
    return targets[deviceResources.currentBackBufferIndex().coerceIn(targets.indices)]
  }

  private fun createSkiaTargets(size: IntSize): List<SkiaTarget> =
    (0 until Direct3DDeviceResources.BufferCount).map { index ->
      val resource = deviceResources.getBuffer(index)
      val renderTarget =
        BackendRenderTarget.makeDirect3D(
          size.width,
          size.height,
          resource,
          Direct3DDeviceResources.BackBufferFormat,
          sampleCnt = 1,
          levelCnt = 0,
        )
      val surface =
        Surface.makeFromBackendRenderTarget(
          directContext,
          renderTarget,
          SurfaceOrigin.TOP_LEFT,
          SurfaceColorFormat.BGRA_8888,
          ColorSpace.sRGB,
          SurfaceProps(),
        )
      SkiaTarget(size.width, size.height, resource, renderTarget, checkNotNull(surface))
    }

  private fun closeTargets(syncCpu: Boolean) {
    if (syncCpu) {
      currentTarget()?.surface?.let { surface ->
        runCatching { directContext.flushAndSubmit(surface, syncCpu = true) }
      }
    }
    targets.forEach(SkiaTarget::close)
    targets = emptyList()
  }

  override fun close() {
    closeTargets(syncCpu = true)
    directContext.close()
    deviceResources.close()
  }

  private data class SkiaTarget(
    val width: Int,
    val height: Int,
    val resource: Long,
    val renderTarget: BackendRenderTarget,
    val surface: Surface,
  ) : AutoCloseable {
    override fun close() {
      surface.close()
      renderTarget.close()
      Com.release(resource)
    }
  }
}
