@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformScreenReader
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.internal.window.GlfwPlatformWindow

internal class GlfwPlatformContext(private val window: GlfwPlatformWindow) : PlatformContext {
  private val fallbackContext = PlatformContext.Empty()
  val textInput: GlfwTextInputService = GlfwTextInputService()
  private val glfwTextToolbar = GlfwTextToolbar()
  private val mutableWindowInfo = GlfwComposeWindowInfo()
  override val windowInfo: WindowInfo = mutableWindowInfo

  override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing =
    textInput.startInputMethod(request)

  override fun requestFocus(): Boolean {
    window.requestFocus()
    return true
  }

  override val hasNonTranslationComponents: Boolean
    get() = false

  override fun convertLocalToWindowPosition(localPosition: Offset): Offset =
    localPosition

  override fun convertWindowToLocalPosition(positionInWindow: Offset): Offset =
    positionInWindow

  override fun convertLocalToScreenPosition(localPosition: Offset): Offset =
    localPosition + window.screenOrigin()

  override fun convertScreenToLocalPosition(positionOnScreen: Offset): Offset =
    positionOnScreen - window.screenOrigin()

  override fun setPointerIcon(pointerIcon: PointerIcon) {
    window.setPointerIcon(pointerIcon)
  }

  // TODO: Bind to OS accessibility APIs; GLFW does not expose screen reader state.
  override val screenReader: PlatformScreenReader
    get() = fallbackContext.screenReader

  // TODO: Provide real lifecycle and ViewModel owners from the GLFW window lifecycle.
  override val architectureComponentsOwner: PlatformArchitectureComponentsOwner
    get() = fallbackContext.architectureComponentsOwner

  // TODO: Add a window option backed by GLFW_TRANSPARENT_FRAMEBUFFER, then report it here.
  override val isWindowTransparent: Boolean
    get() = fallbackContext.isWindowTransparent

  // TODO: Revisit when platform views or layer diagnostics need out-of-layout draw bounds.
  override val measureDrawLayerBounds: Boolean
    get() = fallbackContext.measureDrawLayerBounds

  // TODO: GLFW does not provide desktop gesture timings or touch slop; choose explicit host defaults.
  override val viewConfiguration: ViewConfiguration
    get() = fallbackContext.viewConfiguration

  // TODO: GLFW exposes keyboard/mouse events, but not rich touch/stylus input-mode changes.
  override val inputModeManager: InputModeManager
    get() = fallbackContext.inputModeManager

  // TODO: Legacy API; keep startInputMethod as the active path and implement IME/preedit separately.
  @Suppress("DEPRECATION")
  override val textInputService: PlatformTextInputService
    get() = fallbackContext.textInputService

  override val textToolbar: TextToolbar
    get() = glfwTextToolbar

  // TODO: Revisit with multi-window and parent/owned popup focus behavior.
  override val parentFocusManager: FocusManager
    get() = fallbackContext.parentFocusManager

  // TODO: GLFW has file drop callbacks; adapt them into Compose's drag-and-drop manager.
  override val dragAndDropManager: PlatformDragAndDropManager
    get() = fallbackContext.dragAndDropManager

  // TODO: GLFW does not expose Wayland decoration/safe-area insets in Compose's window-inset model.
  override val windowInsets: PlatformWindowInsets
    get() = fallbackContext.windowInsets

  // TODO: Requires OS power-management APIs; GLFW does not provide keep-screen-on controls.
  override var isKeepScreenOnEnabled: Boolean
    get() = fallbackContext.isKeepScreenOnEnabled
    set(value) {
      fallbackContext.isKeepScreenOnEnabled = value
    }

  // TODO: Requires compositor/display frame-rate APIs; GLFW does not provide this directly.
  override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
    fallbackContext.voteFrameRate(frameRate, frameRateCategory)
  }

  // TODO: Expose test root tracking hooks for host-level tests.
  override val rootForTestListener: PlatformContext.RootForTestListener?
    get() = fallbackContext.rootForTestListener

  // TODO: Expose semantics tracking hooks; accessibility still needs OS-specific integration.
  override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?
    get() = fallbackContext.semanticsOwnerListener

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

  @Composable
  fun TextToolbarContent() {
    glfwTextToolbar.Content()
  }
}

private class GlfwComposeWindowInfo : WindowInfo {
  override var isWindowFocused: Boolean by mutableStateOf(true)
  override var keyboardModifiers: PointerKeyboardModifiers by mutableStateOf(PointerKeyboardModifiers())
  override var containerSize: IntSize by mutableStateOf(IntSize.Zero)
  override var containerDpSize: DpSize by mutableStateOf(DpSize.Zero)
}

private fun GlfwPlatformWindow.screenOrigin(): Offset =
  if (supportsWindowPosition) {
    error("Window position support must define screen conversion in framebuffer-pixel coordinates.")
  } else {
    // Wayland deliberately does not expose global window coordinates through GLFW.
    Offset.Zero
  }
