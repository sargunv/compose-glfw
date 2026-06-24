@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.platform.PlatformScreenReader
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.enableSavedStateHandles
import dev.sargunv.composeglfw.TextToolbarContent
import dev.sargunv.composeglfw.internal.window.PlatformWindow

internal class HostPlatformContext(
  private var window: PlatformWindow,
  textToolbarContent: TextToolbarContent,
  initialVisible: Boolean,
  initialMinimized: Boolean,
) : PlatformContext {
  private val fallbackContext = PlatformContext.Empty()
  val textInput: TextInputService = TextInputService()
  private val textToolbarAdapter = TextToolbarAdapter(textToolbarContent)
  private val hostInputModeManager = HostInputModeManager()
  private val mutableWindowInfo = ComposeWindowInfoState()
  private val rootForTestRegistry = RootForTestRegistry()
  private val semanticsOwnerRegistry = SemanticsOwnerRegistry()
  private val architectureOwner =
    DefaultArchitectureComponentsOwner(enforceMainThread = false).apply {
      enableSavedStateHandles()
      setLifecycleState(State.CREATED)
    }
  private var lifecycleDestroyed = false
  private var lifecycleVisible = initialVisible
  private var lifecycleMinimized = initialMinimized || window.isIconified
  private var lastLifecycleState: State? = null
  var systemTheme: SystemTheme by mutableStateOf(SystemTheme.Unknown)

  override val windowInfo: WindowInfo = mutableWindowInfo

  override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing =
    textInput.startInputMethod(request)

  override fun requestFocus(): Boolean {
    window.requestFocus()
    return true
  }

  override val hasNonTranslationComponents: Boolean
    get() = false

  override fun convertLocalToWindowPosition(localPosition: Offset): Offset = localPosition

  override fun convertWindowToLocalPosition(positionInWindow: Offset): Offset = positionInWindow

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

  override val architectureComponentsOwner: PlatformArchitectureComponentsOwner
    get() = architectureOwner

  override val isWindowTransparent: Boolean
    get() = window.isTransparent

  // GLFW content is rendered directly into the window framebuffer; no extra layer bounds are
  // needed for the current host.
  override val measureDrawLayerBounds: Boolean
    get() = fallbackContext.measureDrawLayerBounds

  // GLFW does not expose host gesture timings or slop; Compose's Skiko defaults are appropriate
  // here.
  override val viewConfiguration: ViewConfiguration
    get() = fallbackContext.viewConfiguration

  override val inputModeManager: InputModeManager
    get() = hostInputModeManager

  // Legacy Compose API. The active text-input path is startInputMethod.
  @Suppress("DEPRECATION")
  override val textInputService: PlatformTextInputService
    get() = fallbackContext.textInputService

  override val textToolbar: TextToolbar
    get() = textToolbarAdapter

  // Compose calls parentFocusManager when focus traversal leaves this scene, such as tabbing past
  // the first/last focus target. A GLFW window is the root host, so there is no parent focus target
  // to move into; traversal stops at the window boundary.
  override val parentFocusManager: FocusManager
    get() = fallbackContext.parentFocusManager

  // TODO: GLFW has file drop callbacks; adapt them into Compose's drag-and-drop manager.
  override val dragAndDropManager: PlatformDragAndDropManager
    get() = fallbackContext.dragAndDropManager

  // Desktop window content is already the safe drawable area: OS title bars and borders live
  // outside the GLFW content area.
  override val windowInsets: PlatformWindowInsets
    get() = fallbackContext.windowInsets

  // TODO: Requires OS power-management APIs; GLFW does not provide keep-screen-on controls.
  override var isKeepScreenOnEnabled: Boolean
    get() = fallbackContext.isKeepScreenOnEnabled
    set(value) {
      fallbackContext.isKeepScreenOnEnabled = value
    }

  private var frameRateVote: FrameRateVote? = null

  override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
    frameRateVote = FrameRateVote(frameRate, frameRateCategory)
  }

  override val rootForTestListener: PlatformContext.RootForTestListener?
    get() = rootForTestRegistry

  override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?
    get() = semanticsOwnerRegistry

  val rootsForTest: Set<PlatformRootForTest>
    get() = rootForTestRegistry.roots

  val semanticsOwners: Set<SemanticsOwner>
    get() = semanticsOwnerRegistry.owners

  fun setRootForTestListener(listener: PlatformContext.RootForTestListener?) {
    rootForTestRegistry.setListener(listener)
  }

  fun setSemanticsOwnerListener(listener: PlatformContext.SemanticsOwnerListener?) {
    semanticsOwnerRegistry.setListener(listener)
  }

  fun updateWindowInfo() {
    mutableWindowInfo.isWindowFocused = window.isFocused
    updateLifecycle()
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

  fun updateKeyboardInputMode() {
    hostInputModeManager.switchToKeyboardInputMode()
  }

  fun updatePointerInputMode() {
    hostInputModeManager.switchToPointerInputMode()
  }

  fun updateFocus(focused: Boolean) {
    mutableWindowInfo.isWindowFocused = focused
    updateLifecycle()
  }

  fun updateLifecycleState(
    visible: Boolean = lifecycleVisible,
    minimized: Boolean = lifecycleMinimized,
  ) {
    lifecycleVisible = visible
    lifecycleMinimized = minimized
    updateLifecycle()
  }

  fun updateTextToolbarContent(content: TextToolbarContent) {
    textToolbarAdapter.updateContent(content)
  }

  fun updateWindow(
    window: PlatformWindow,
    visible: Boolean,
    minimized: Boolean,
  ) {
    this.window = window
    lifecycleVisible = visible
    lifecycleMinimized = minimized || window.isIconified
    updateWindowInfo()
  }

  fun destroyLifecycle() {
    if (!lifecycleDestroyed) {
      lifecycleDestroyed = true
      architectureOwner.setLifecycleState(State.DESTROYED)
      lastLifecycleState = State.DESTROYED
    }
  }

  fun consumeFrameRateVote(): FrameRateVote? = frameRateVote.also {
    frameRateVote = null
  }

  @Composable
  fun TextToolbarContent() {
    textToolbarAdapter.Content()
  }

  private fun updateLifecycle() {
    if (!lifecycleDestroyed) {
      val state =
        when {
          !lifecycleVisible || lifecycleMinimized -> State.CREATED
          mutableWindowInfo.isWindowFocused -> State.RESUMED
          else -> State.STARTED
        }
      if (state != lastLifecycleState) {
        architectureOwner.setLifecycleState(state)
        lastLifecycleState = state
      }
    }
  }
}

internal data class FrameRateVote(
  val frameRate: Float,
  val frameRateCategory: Float,
) {
  fun targetFramesPerSecond(displayRefreshRate: Int?): Float? =
    when {
      frameRate.isFinite() && frameRate > 0f -> frameRate
      frameRateCategory == FrameRateCategoryNormal -> minOf(displayRefreshRate ?: 60, 60).toFloat()
      frameRateCategory == FrameRateCategoryHigh -> null
      else -> null
    }
}

private const val FrameRateCategoryNormal = -3f
private const val FrameRateCategoryHigh = -4f

private class ComposeWindowInfoState : WindowInfo {
  override var isWindowFocused: Boolean by mutableStateOf(true)
  override var keyboardModifiers: PointerKeyboardModifiers by
    mutableStateOf(PointerKeyboardModifiers())
  override var containerSize: IntSize by mutableStateOf(IntSize.Zero)
  override var containerDpSize: DpSize by mutableStateOf(DpSize.Zero)
}

private fun PlatformWindow.screenOrigin(): Offset =
  if (supportsWindowPosition) {
    // GLFW exposes window position in screen coordinates, while this host sizes ComposeScene in
    // framebuffer pixels. Convert the origin with the same ratio used for pointer coordinates.
    Offset(
      x = windowPosition.x * framebufferSize.width.toFloat() / windowSize.width,
      y = windowPosition.y * framebufferSize.height.toFloat() / windowSize.height,
    )
  } else {
    // Wayland deliberately does not expose global window coordinates through GLFW.
    Offset.Zero
  }
