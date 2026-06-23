@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package dev.sargunv.composeglfw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.EmptyCoroutineContext
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.GLAssembledInterface
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skia.makeGLWithInterface
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_EGL_CONTEXT_API
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetError
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetPlatform
import org.lwjgl.glfw.GLFW.glfwGetVersionString
import org.lwjgl.glfw.GLFW.glfwGetWindowContentScale
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwInitHint
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwSetErrorCallback
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_RENDERER
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_VENDOR
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.GL_STENCIL_BITS
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL11.glGetString
import org.lwjgl.opengl.GL11.glReadPixels
import org.lwjgl.opengl.GL13.GL_SAMPLES
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING
import org.lwjgl.system.APIUtil.apiCreateLibrary
import org.lwjgl.system.APIUtil.apiGetFunctionAddress
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.system.SharedLibrary

internal class GlfwComposeHost(private val title: String, private val width: Int, private val height: Int) :
  AutoCloseable {
  private var window = NULL
  private var procLibrary: SharedLibrary? = null
  private var glInterface: GLAssembledInterface? = null
  private var directContext: DirectContext? = null
  private var skiaTarget: SkiaTarget? = null
  private var scene: ComposeScene? = null
  private var framebuffer = FramebufferSize(width, height)
  private var windowSize = WindowSize(width, height)
  private var contentScale = 1f
  private var renderRequested = true
  private var renderedFrames = 0
  private val exitAfterFrames = System.getenv("COMPOSE_GLFW_EXIT_AFTER_FRAMES")?.toIntOrNull()
  private var mousePressed = false
  private var lastMouse = Offset.Zero

  fun run() {
    createWindow()
    createScene()
    while (!glfwWindowShouldClose(window)) {
      glfwPollEvents()
      resizeIfNeeded()
      render()
    }
  }

  override fun close() {
    scene?.close()
    scene = null
    skiaTarget?.close()
    skiaTarget = null
    directContext?.close()
    directContext = null
    glInterface?.close()
    glInterface = null
    procLibrary?.close()
    procLibrary = null
    if (window != NULL) {
      glfwDestroyWindow(window)
      window = NULL
    }
    glfwTerminate()
    glfwSetErrorCallback(null)?.free()
  }

  private fun createWindow() {
    glfwSetErrorCallback { code, description -> System.err.println("GLFW error $code: ${memUTF8(description)}") }
    if (System.getenv("WAYLAND_DISPLAY") != null) {
      glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND)
    }
    check(glfwInit()) { "GLFW initialization failed: ${glfwGetError(null)}" }

    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
    glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    window = glfwCreateWindow(width, height, title, NULL, NULL)
    check(window != NULL) { "GLFW window creation failed: ${glfwGetError(null)}" }
    glfwMakeContextCurrent(window)
    glfwSwapInterval(1)
    GL.createCapabilities()
    println("OpenGL vendor: ${glGetString(GL_VENDOR)}")
    println("OpenGL renderer: ${glGetString(GL_RENDERER)}")
    println("OpenGL version: ${glGetString(GL_VERSION)}")

    readWindowSize()
    readFramebufferSize()
    readContentScale()
    directContext = createDirectContext()
    skiaTarget = createSkiaTarget(framebuffer)
    installCallbacks()

    val platform = platformName()
    println("GLFW ${glfwGetVersionString()}")
    println("GLFW platform: $platform")
    println("Wayland display: ${System.getenv("WAYLAND_DISPLAY") ?: "<unset>"}")
    if (glfwGetPlatform() == GLFW_PLATFORM_X11) {
      System.err.println("Warning: GLFW selected X11/XWayland. Set WAYLAND_DISPLAY and use GLFW 3.4 Wayland support.")
    }
  }

  private fun createScene() {
    val scene =
      CanvasLayersComposeScene(
        density = Density(contentScale),
        layoutDirection = LayoutDirection.Ltr,
        size = IntSize(framebuffer.width, framebuffer.height),
        coroutineContext = EmptyCoroutineContext,
        invalidate = { renderRequested = true },
      )
    scene.setContent {
      ComposeGlfwApp(HostInfo(platform = platformName(), displayName = System.getenv("WAYLAND_DISPLAY") ?: "<unset>"))
    }
    this.scene = scene
  }

  private fun render() {
    val scene = scene ?: return
    val target = skiaTarget ?: return
    if (framebuffer.width <= 0 || framebuffer.height <= 0) {
      return
    }
    if (!renderRequested && !scene.hasInvalidations()) {
      return
    }
    renderRequested = false

    scene.render(target.surface.canvas.asComposeCanvas(), System.nanoTime())
    target.surface.flushAndSubmit()
    renderedFrames++
    if (exitAfterFrames != null) {
      printSmokePixel()
      if (renderedFrames >= exitAfterFrames) {
        glfwSetWindowShouldClose(window, true)
      }
    }
    glfwSwapBuffers(window)
  }

  private fun printSmokePixel() {
    if (renderedFrames != 1 || framebuffer.width <= 0 || framebuffer.height <= 0) {
      return
    }
    MemoryStack.stackPush().use { stack ->
      val pixel = stack.malloc(4)
      glReadPixels(framebuffer.width / 2, framebuffer.height / 2, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel)
      val r = pixel[0].toInt() and 0xff
      val g = pixel[1].toInt() and 0xff
      val b = pixel[2].toInt() and 0xff
      val a = pixel[3].toInt() and 0xff
      println("Smoke frame: ${framebuffer.width}x${framebuffer.height}, center rgba=($r,$g,$b,$a)")
    }
  }

  private fun installCallbacks() {
    glfwSetFramebufferSizeCallback(window) { _, _, _ ->
      readFramebufferSize()
      renderRequested = true
    }
    glfwSetCursorPosCallback(window) { _, x, y ->
      lastMouse =
        Offset(
          (x * framebuffer.width / windowSize.width).toFloat(),
          (y * framebuffer.height / windowSize.height).toFloat(),
        )
      sendPointer(PointerEventType.Move)
    }
    glfwSetMouseButtonCallback(window) { _, button, action, _ ->
      if (button == GLFW_MOUSE_BUTTON_1 && (action == GLFW_PRESS || action == GLFW_RELEASE)) {
        mousePressed = action == GLFW_PRESS
        sendPointer(if (mousePressed) PointerEventType.Press else PointerEventType.Release, PointerButton.Primary)
      }
    }
  }

  private fun sendPointer(type: PointerEventType, button: PointerButton? = null) {
    scene?.sendPointerEvent(
      eventType = type,
      position = lastMouse,
      timeMillis = System.currentTimeMillis(),
      type = PointerType.Mouse,
      buttons = PointerButtons(if (mousePressed) 1 else 0),
      keyboardModifiers = PointerKeyboardModifiers(0),
      nativeEvent = null,
      button = button,
    )
    renderRequested = true
  }

  private fun resizeIfNeeded() {
    readWindowSize()
    readContentScale()
    val current = framebuffer
    val target = skiaTarget
    if (target == null || current.width != target.width || current.height != target.height) {
      skiaTarget?.close()
      skiaTarget = null
      if (current.width > 0 && current.height > 0) {
        skiaTarget = createSkiaTarget(current)
        scene?.size = IntSize(current.width, current.height)
      }
      renderRequested = true
    }
    val scene = scene
    if (scene != null && scene.density.density != contentScale) {
      scene.density = Density(contentScale)
      renderRequested = true
    }
  }

  private fun createSkiaTarget(size: FramebufferSize): SkiaTarget {
    val context = checkNotNull(directContext)
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
        context,
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
    } catch (error: RuntimeException) {
      val libraryPath =
        System.getProperty("compose.glfw.gl.proc.library")
          ?: "build/native/glproc/libcompose_gl_proc.so"
      val library = apiCreateLibrary(libraryPath)
      val getProcAddress = apiGetFunctionAddress(library, "compose_glfw_get_proc")
      val assembledInterface = GLAssembledInterface.createFromNativePointers(NULL, getProcAddress)
      procLibrary = library
      glInterface = assembledInterface
      println("Skia default GL context creation failed; using explicit EGL/GL proc loader: ${error.message}")
      DirectContext.makeGLWithInterface(assembledInterface)
    }

  private fun readFramebufferSize() {
    MemoryStack.stackPush().use { stack ->
      val w = stack.mallocInt(1)
      val h = stack.mallocInt(1)
      glfwGetFramebufferSize(window, w, h)
      framebuffer = FramebufferSize(w[0].coerceAtLeast(0), h[0].coerceAtLeast(0))
    }
  }

  private fun readWindowSize() {
    MemoryStack.stackPush().use { stack ->
      val w = stack.mallocInt(1)
      val h = stack.mallocInt(1)
      glfwGetWindowSize(window, w, h)
      windowSize = WindowSize(w[0].coerceAtLeast(1), h[0].coerceAtLeast(1))
    }
  }

  private fun readContentScale() {
    MemoryStack.stackPush().use { stack ->
      val x = stack.mallocFloat(1)
      val y = stack.mallocFloat(1)
      glfwGetWindowContentScale(window, x, y)
      contentScale = maxOf(x[0], y[0], 1f)
    }
  }

  private fun platformName(): String =
    when (glfwGetPlatform()) {
      GLFW_PLATFORM_WAYLAND -> "Wayland"
      GLFW_PLATFORM_X11 -> "X11"
      else -> "Unknown"
    }

  private data class FramebufferSize(val width: Int, val height: Int)

  private data class WindowSize(val width: Int, val height: Int)

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
