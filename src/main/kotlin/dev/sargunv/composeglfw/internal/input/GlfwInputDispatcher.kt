package dev.sargunv.composeglfw.internal.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import dev.sargunv.composeglfw.internal.platform.GlfwTextInputService
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.GlfwPlatformWindow
import org.lwjgl.glfw.GLFW.GLFW_KEY_SCROLL_LOCK
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.glfwSetCharCallback
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback
import org.lwjgl.glfw.GLFW.glfwSetScrollCallback

// Compose's desktop scroll config normally sees AWT's scrollAmount, commonly 3 lines per wheel step.
// GLFW gives unit offsets without that metadata, so apply the same baseline before forwarding.
private const val GlfwScrollAmount = 3f

internal class GlfwInputDispatcher(
  private val window: GlfwPlatformWindow,
  private val scene: ComposeWindowScene,
  textInput: GlfwTextInputService,
  private val requestRender: () -> Unit,
) : AutoCloseable {
  private var mousePressed = false
  private var lastMouse = Offset.Zero
  private var currentMods = 0
  private var scrollLockOn = false

  init {
    glfwSetCursorPosCallback(window.handle) { _, x, y ->
      updateMousePosition(x, y)
      sendPointer(PointerEventType.Move)
    }
    glfwSetMouseButtonCallback(window.handle) { _, button, action, mods ->
      currentMods = mods
      if (button == GLFW_MOUSE_BUTTON_1 && (action == GLFW_PRESS || action == GLFW_RELEASE)) {
        mousePressed = action == GLFW_PRESS
        sendPointer(if (mousePressed) PointerEventType.Press else PointerEventType.Release, PointerButton.Primary)
      }
    }
    glfwSetScrollCallback(window.handle) { _, x, y ->
      sendPointer(
        type = PointerEventType.Scroll,
        scrollDelta = Offset(x.toFloat(), -y.toFloat()) * GlfwScrollAmount,
      )
    }
    glfwSetKeyCallback(window.handle) { _, key, scancode, action, mods ->
      currentMods = mods
      if (key == GLFW_KEY_SCROLL_LOCK && action == GLFW_PRESS) {
        scrollLockOn = !scrollLockOn
      }
      val event = glfwKeyEvent(key, scancode, action, mods)
      if (event != null) {
        scene.sendKeyEvent(event)
        requestRender()
      }
    }
    glfwSetCharCallback(window.handle) { _, codePoint ->
      if (textInput.commit(codePoint)) {
        requestRender()
      }
    }
  }

  override fun close() {
    glfwSetCursorPosCallback(window.handle, null)?.free()
    glfwSetMouseButtonCallback(window.handle, null)?.free()
    glfwSetScrollCallback(window.handle, null)?.free()
    glfwSetKeyCallback(window.handle, null)?.free()
    glfwSetCharCallback(window.handle, null)?.free()
  }

  private fun updateMousePosition(x: Double, y: Double) {
    val framebuffer = window.framebufferSize
    val windowSize = window.windowSize
    lastMouse =
      Offset(
        (x * framebuffer.width / windowSize.width).toFloat(),
        (y * framebuffer.height / windowSize.height).toFloat(),
      )
  }

  private fun sendPointer(
    type: PointerEventType,
    button: PointerButton? = null,
    scrollDelta: Offset = Offset.Zero,
  ) {
    scene.sendPointerEvent(
      event = type,
      position = lastMouse,
      scrollDelta = scrollDelta,
      button = button,
      buttons = PointerButtons(if (mousePressed) 1 else 0),
      keyboardModifiers = glfwKeyboardModifiers(currentMods, scrollLockOn),
    )
    requestRender()
  }
}
