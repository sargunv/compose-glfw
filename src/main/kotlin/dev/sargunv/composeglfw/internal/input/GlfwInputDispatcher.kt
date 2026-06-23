package dev.sargunv.composeglfw.internal.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.window.GlfwPlatformWindow
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback

internal class GlfwInputDispatcher(
  private val window: GlfwPlatformWindow,
  private val scene: ComposeWindowScene,
  private val requestRender: () -> Unit,
) : AutoCloseable {
  private var mousePressed = false
  private var lastMouse = Offset.Zero

  init {
    glfwSetCursorPosCallback(window.handle) { _, x, y ->
      updateMousePosition(x, y)
      sendPointer(PointerEventType.Move)
    }
    glfwSetMouseButtonCallback(window.handle) { _, button, action, _ ->
      if (button == GLFW_MOUSE_BUTTON_1 && (action == GLFW_PRESS || action == GLFW_RELEASE)) {
        mousePressed = action == GLFW_PRESS
        sendPointer(if (mousePressed) PointerEventType.Press else PointerEventType.Release, PointerButton.Primary)
      }
    }
  }

  override fun close() {
    glfwSetCursorPosCallback(window.handle, null)?.free()
    glfwSetMouseButtonCallback(window.handle, null)?.free()
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

  private fun sendPointer(type: PointerEventType, button: PointerButton? = null) {
    scene.sendPointerEvent(
      event = type,
      position = lastMouse,
      button = button,
      buttons = PointerButtons(if (mousePressed) 1 else 0),
    )
    requestRender()
  }
}
