package dev.sargunv.composeglfw.internal.application

import dev.sargunv.composeglfw.GlfwRenderBackend
import dev.sargunv.composeglfw.GlfwWindowInfo
import dev.sargunv.composeglfw.GlfwWindowSpec
import dev.sargunv.composeglfw.internal.input.GlfwInputDispatcher
import dev.sargunv.composeglfw.internal.platform.glfwPlatform
import dev.sargunv.composeglfw.internal.render.opengl.OpenGlRenderBackend
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.scene.GlfwWindowScopeImpl
import dev.sargunv.composeglfw.internal.window.GlfwPlatformWindow
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback

internal class GlfwWindowHost(private val spec: GlfwWindowSpec) : AutoCloseable {
  private val window = GlfwPlatformWindow(spec.title, spec.size, spec.options)
  private val renderBackend = OpenGlRenderBackend(window)
  private val scope = GlfwWindowScopeImpl(currentInfo(), renderBackend.interop)
  private val scene =
    ComposeWindowScene(
      initialDensity = window.contentScale,
      initialSize = window.framebufferSize,
      scope = scope,
      content = spec.content,
      invalidate = ::requestRender,
    )
  private val input = GlfwInputDispatcher(window, scene, ::requestRender)
  private var lastFramebufferSize = window.framebufferSize
  private var lastContentScale = window.contentScale
  private var renderRequested = true

  val shouldClose: Boolean
    get() = window.shouldClose

  init {
    glfwSetFramebufferSizeCallback(window.handle) { _, _, _ ->
      window.refreshSizes()
      updateSceneMetrics()
      requestRender()
    }
  }

  fun updateAndRender() {
    window.refreshSizes()
    updateSceneMetrics()
    if (window.framebufferSize.width <= 0 || window.framebufferSize.height <= 0) {
      return
    }
    if (!renderRequested && !scene.hasInvalidations) {
      return
    }
    renderRequested = false
    renderBackend.render(scene, System.nanoTime())
  }

  override fun close() {
    glfwSetFramebufferSizeCallback(window.handle, null)?.free()
    input.close()
    scene.close()
    renderBackend.close()
    window.close()
  }

  private fun requestRender() {
    renderRequested = true
  }

  private fun updateSceneMetrics() {
    val framebuffer = window.framebufferSize
    if (framebuffer != lastFramebufferSize) {
      lastFramebufferSize = framebuffer
      renderBackend.resize(framebuffer)
      scene.resize(framebuffer)
      requestRender()
    }
    if (window.contentScale != lastContentScale) {
      lastContentScale = window.contentScale
      scene.updateDensity(window.contentScale)
      requestRender()
    }
    scope.updateInfo(currentInfo())
  }

  private fun currentInfo(): GlfwWindowInfo =
    GlfwWindowInfo(
      platform = glfwPlatform(),
      displayName = System.getenv("WAYLAND_DISPLAY"),
      renderBackend = GlfwRenderBackend.OPENGL,
      framebufferWidth = window.framebufferSize.width,
      framebufferHeight = window.framebufferSize.height,
      windowWidth = window.windowSize.width,
      windowHeight = window.windowSize.height,
      contentScale = window.contentScale,
    )
}
