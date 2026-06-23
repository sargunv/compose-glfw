@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.internal.window.GlfwPlatformWindow

internal class GlfwPlatformContext(private val window: GlfwPlatformWindow) : PlatformContext by PlatformContext.Empty() {
  val textInput: GlfwTextInputService = GlfwTextInputService()
  private val mutableWindowInfo = GlfwComposeWindowInfo()
  override val windowInfo: WindowInfo = mutableWindowInfo

  override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing =
    textInput.startInputMethod(request)

  override fun requestFocus(): Boolean {
    window.requestFocus()
    return true
  }

  override val hasNonTranslationComponents: Boolean
    get() = window.framebufferSize != window.windowSize

  override fun convertLocalToWindowPosition(localPosition: Offset): Offset =
    localPosition.framebufferLocalToWindow(window)

  override fun convertWindowToLocalPosition(positionInWindow: Offset): Offset =
    positionInWindow.windowToFramebufferLocal(window)

  override fun convertLocalToScreenPosition(localPosition: Offset): Offset =
    localPosition.framebufferLocalToWindow(window) + window.screenOrigin()

  override fun convertScreenToLocalPosition(positionOnScreen: Offset): Offset =
    (positionOnScreen - window.screenOrigin()).windowToFramebufferLocal(window)

  override fun setPointerIcon(pointerIcon: PointerIcon) {
    window.setPointerIcon(pointerIcon)
  }

  // PlatformContext members still backed by PlatformContext.Empty():
  //
  // override val screenReader: PlatformScreenReader
  // override val architectureComponentsOwner: PlatformArchitectureComponentsOwner
  // override val isWindowTransparent: Boolean
  // override val measureDrawLayerBounds: Boolean
  // override val viewConfiguration: ViewConfiguration
  // override val inputModeManager: InputModeManager
  // override val textInputService: PlatformTextInputService
  // override val textToolbar: TextToolbar
  // override val parentFocusManager: FocusManager
  // override val dragAndDropManager: PlatformDragAndDropManager
  // override val windowInsets: PlatformWindowInsets
  // override var isKeepScreenOnEnabled: Boolean
  // override fun voteFrameRate(frameRate: Float, frameRateCategory: Float)
  // override val rootForTestListener: PlatformContext.RootForTestListener?
  // override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?

  fun updateWindowInfo() {
    mutableWindowInfo.isWindowFocused = window.isFocused
    // WindowInfo describes the Compose scene container, which we size to framebuffer pixels.
    mutableWindowInfo.containerSize = window.framebufferSize
    mutableWindowInfo.containerDpSize =
      DpSize(
        (window.framebufferSize.width / window.contentScale).dp,
        (window.framebufferSize.height / window.contentScale).dp,
      )
  }

  fun updateKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
    mutableWindowInfo.keyboardModifiers = modifiers
  }

  fun updateFocus(focused: Boolean) {
    mutableWindowInfo.isWindowFocused = focused
  }
}

private class GlfwComposeWindowInfo : WindowInfo {
  override var isWindowFocused: Boolean by mutableStateOf(true)
  override var keyboardModifiers: PointerKeyboardModifiers by mutableStateOf(PointerKeyboardModifiers())
  override var containerSize: IntSize by mutableStateOf(IntSize.Zero)
  override var containerDpSize: DpSize by mutableStateOf(DpSize.Zero)
}

private fun IntOffset.toOffset(): Offset = Offset(x.toFloat(), y.toFloat())

private fun GlfwPlatformWindow.screenOrigin(): Offset =
  if (supportsWindowPosition) {
    windowPosition.toOffset()
  } else {
    // Wayland deliberately does not expose global window coordinates through GLFW.
    Offset.Zero
  }

private fun Offset.scale(from: IntSize, to: IntSize): Offset =
  Offset(
    x = x * to.width / from.width,
    y = y * to.height / from.height,
  )

// ComposeScene local positions are framebuffer pixels because scene.size is the framebuffer size.
private fun Offset.framebufferLocalToWindow(window: GlfwPlatformWindow): Offset =
  scale(from = window.framebufferSize, to = window.windowSize)

// Position-in-window values are content-area-relative GLFW screen-coordinate units.
private fun Offset.windowToFramebufferLocal(window: GlfwPlatformWindow): Offset =
  scale(from = window.windowSize, to = window.framebufferSize)
