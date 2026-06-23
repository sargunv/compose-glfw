package dev.sargunv.composeglfw.internal.application

import dev.sargunv.composeglfw.GlfwRenderBackend
import dev.sargunv.composeglfw.GlfwWindowInfo
import dev.sargunv.composeglfw.GlfwWindowSpec
import dev.sargunv.composeglfw.internal.input.GlfwInputDispatcher
import dev.sargunv.composeglfw.internal.platform.GlfwPlatformContext
import dev.sargunv.composeglfw.internal.platform.glfwPlatform
import dev.sargunv.composeglfw.internal.render.opengl.OpenGlRenderBackend
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.scene.GlfwWindowScopeImpl
import dev.sargunv.composeglfw.internal.window.GlfwPlatformWindow
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback

internal class GlfwWindowHost(
  spec: GlfwWindowSpec,
  uiDispatcher: GlfwUiDispatcher,
) : AutoCloseable {
  private val window = GlfwPlatformWindow(spec.title, spec.size, spec.options)
  private val renderBackend = OpenGlRenderBackend(window)
  private val platformContext = GlfwPlatformContext(window)
  private val scope = GlfwWindowScopeImpl(currentInfo(), renderBackend.interop)
  private val scene =
    ComposeWindowScene(
      initialDensity = window.contentScale,
      // Compose layout/rendering uses framebuffer pixels to match the OpenGL/Skia target.
      initialSize = window.framebufferSize,
      platformContext = platformContext,
      coroutineContext = uiDispatcher,
      scope = scope,
      content = spec.content,
      invalidate = ::requestRender,
    )
  private val input =
    GlfwInputDispatcher(
      window = window,
      scene = scene,
      textInput = platformContext.textInput,
      onKeyboardModifiers = platformContext::updateKeyboardModifiers,
      requestRender = ::requestRender,
    )
  private var lastFramebufferSize = window.framebufferSize
  private var lastContentScale = window.contentScale
  private var renderRequested = true

  val shouldClose: Boolean
    get() = window.shouldClose

  init {
    platformContext.updateWindowInfo()
    glfwSetFramebufferSizeCallback(window.handle) { _, _, _ ->
      window.refreshSizes()
      updateSceneMetrics()
      requestRender()
    }
    glfwSetWindowFocusCallback(window.handle) { _, focused ->
      platformContext.updateFocus(focused)
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
    glfwSetWindowFocusCallback(window.handle, null)?.free()
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
    platformContext.updateWindowInfo()
    scope.updateInfo(currentInfo())
  }

  private fun currentInfo(): GlfwWindowInfo =
    GlfwWindowInfo(
      platform = glfwPlatform(),
      displayName = System.getenv("WAYLAND_DISPLAY"),
      renderBackend = GlfwRenderBackend.OPENGL,
      // Framebuffer dimensions are physical drawable pixels.
      framebufferWidth = window.framebufferSize.width,
      framebufferHeight = window.framebufferSize.height,
      // Window dimensions are GLFW screen-coordinate content-area units.
      windowWidth = window.windowSize.width,
      windowHeight = window.windowSize.height,
      contentScale = window.contentScale,
    )
}
