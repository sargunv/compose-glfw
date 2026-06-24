package dev.sargunv.composeglfw.internal.application

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import dev.sargunv.composeglfw.DisplayServer
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.RenderBackend
import dev.sargunv.composeglfw.TextToolbarContent
import dev.sargunv.composeglfw.WindowOptions
import dev.sargunv.composeglfw.WindowPlacement
import dev.sargunv.composeglfw.WindowPosition
import dev.sargunv.composeglfw.WindowState
import dev.sargunv.composeglfw.internal.input.InputDispatcher
import dev.sargunv.composeglfw.internal.platform.HostPlatformContext
import dev.sargunv.composeglfw.internal.platform.NfdFilePicker
import dev.sargunv.composeglfw.internal.platform.SystemThemeProvider
import dev.sargunv.composeglfw.internal.platform.currentDisplayServer
import dev.sargunv.composeglfw.internal.render.RenderBackendDriver
import dev.sargunv.composeglfw.internal.render.createRenderBackend
import dev.sargunv.composeglfw.internal.render.currentWindowClientApi
import dev.sargunv.composeglfw.internal.scene.ComposeWindowScene
import dev.sargunv.composeglfw.internal.scene.WindowScopeImpl
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import dev.sargunv.composeglfw.internal.window.PlatformWindowBounds
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowIconifyCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowMaximizeCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowPosCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowRefreshCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback

internal class WindowHost(
  request: WindowRequest,
  private val uiDispatcher: UiDispatcher,
  private val wakeEventLoop: () -> Unit,
) : AutoCloseable {
  private val onCloseRequest = request.onCloseRequest
  private var state = request.state
  private var lastTextToolbar: TextToolbarContent = request.options.textToolbar
  private var config =
    WindowPeerConfig(
      title = request.title,
      size = request.state.size,
      visible = request.visible,
      undecorated = request.undecorated,
      transparent = request.transparent,
      resizable = request.resizable,
      enabled = request.enabled,
      focusOnShow = request.focusOnShow,
      alwaysOnTop = request.alwaysOnTop,
    )
  private var onPreviewKeyEvent = request.onPreviewKeyEvent
  private var onKeyEvent = request.onKeyEvent
  private var lastAppliedStateSize = request.state.size.initialConcreteSize()
  private var pendingStateSize: DpSize? = null
  private var hasReappliedPendingStateSize = false
  private var pendingVisiblePreferredSizeRequest: DpSize? = null
  private var isApplyingStateSize = false
  private var lastAppliedPosition: WindowPosition = WindowPosition.PlatformDefault
  private var lastAppliedPlacement: WindowPlacement = WindowPlacement.Floating
  private var pendingPlacement: WindowPlacement? = null
  private var lastAppliedMinimized: Boolean = false
  private var windowedBoundsBeforeFullscreen: PlatformWindowBounds? = null
  private var renderRequested = true
  private var isRenderingFromGlfwCallback = false
  private val shouldRenderDuringBlockedEventProcessing =
    when (currentDisplayServer()) {
      DisplayServer.COCOA,
      DisplayServer.WIN32 -> true
      DisplayServer.WAYLAND,
      DisplayServer.X11 -> false
    }

  private val platformContext: HostPlatformContext
  private val filePicker: NfdFilePicker
  private val scope: WindowScopeImpl
  private val systemThemeProvider: SystemThemeProvider
  private val scene: ComposeWindowScene
  private var peer: WindowPeer
  private var lastFramebufferSize: IntSize
  private var lastContentScale: Float
  private var actualVisible = false

  private val window: PlatformWindow
    get() = peer.window

  val hasPendingWork: Boolean
    get() = canRenderFrame && (renderRequested || scene.hasInvalidations)

  init {
    actualVisible = initialNativeVisibility(config)
    val initialWindow = createPlatformWindow(config, visible = actualVisible)
    val initialRenderBackend = createRenderBackend(initialWindow)
    platformContext =
      HostPlatformContext(
        window = initialWindow,
        textToolbarContent = request.options.textToolbar,
        initialVisible = actualVisible,
        initialMinimized = request.state.isMinimized,
      )
    filePicker =
      NfdFilePicker(
        window = initialWindow,
        checkThread = { operation -> uiDispatcher.checkOwnerThread(operation) },
        runNativeDialog = ::runNativeDialog,
      )
    scope =
      WindowScopeImpl(
        currentInfo(initialWindow, initialRenderBackend.backend),
        initialRenderBackend.interop,
        filePicker,
      )
    systemThemeProvider =
      SystemThemeProvider.create { theme ->
          uiDispatcher.dispatch(
            EmptyCoroutineContext,
            {
              platformContext.systemTheme = theme
              requestRender()
            },
          )
        }
        .also { platformContext.systemTheme = it.systemTheme }
    scene =
      ComposeWindowScene(
        initialDensity = initialWindow.contentScale,
        // Compose layout/rendering uses framebuffer pixels to match the Skia target.
        initialSize = initialWindow.framebufferSize,
        platformContext = platformContext,
        coroutineContext = uiDispatcher,
        scope = scope,
        content = request.content,
        invalidate = ::requestRender,
        checkThread = { operation -> uiDispatcher.checkOwnerThread(operation) },
      )
    peer = attachPeer(initialWindow, initialRenderBackend, config.enabled)
    lastFramebufferSize = initialWindow.framebufferSize
    lastContentScale = initialWindow.contentScale
    platformContext.updateWindowInfo()
    applyStateToWindow()
    syncWindowVisibility()
  }

  fun update(
    title: String,
    state: WindowState,
    visible: Boolean,
    undecorated: Boolean,
    transparent: Boolean,
    resizable: Boolean,
    enabled: Boolean,
    focusOnShow: Boolean,
    alwaysOnTop: Boolean,
    options: WindowOptions,
  ) {
    this.state = state
    if (options.textToolbar !== lastTextToolbar) {
      platformContext.updateTextToolbarContent(options.textToolbar)
      lastTextToolbar = options.textToolbar
    }

    val nextConfig =
      WindowPeerConfig(
        title = title,
        size = state.size,
        visible = visible,
        undecorated = undecorated,
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusOnShow = focusOnShow,
        alwaysOnTop = alwaysOnTop,
      )

    if (nextConfig.transparent != config.transparent) {
      // GLFW reads transparency only when creating the window, so changing it needs a new native
      // window.
      config = nextConfig
      recreatePeer(config)
      applyStateToWindow(forceSize = true)
      syncWindowVisibility()
      return
    }

    if (nextConfig.title != config.title) {
      window.setTitle(nextConfig.title)
    }
    if (nextConfig.undecorated != config.undecorated) {
      window.setDecorated(!nextConfig.undecorated)
    }
    if (nextConfig.resizable != config.resizable) {
      window.setResizable(nextConfig.resizable)
    }
    if (nextConfig.enabled != config.enabled) {
      peer.input.enabled = nextConfig.enabled
    }
    if (nextConfig.focusOnShow != config.focusOnShow) {
      window.setFocusOnShow(nextConfig.focusOnShow)
    }
    if (nextConfig.alwaysOnTop != config.alwaysOnTop) {
      window.setAlwaysOnTop(nextConfig.alwaysOnTop)
    }
    config = nextConfig
    applyStateToWindow()
    syncWindowVisibility()
  }

  fun updateAndRender() {
    applyStateToWindow()
    syncWindowVisibility()
    window.refreshSizes()
    updateSceneMetrics()
    if (applyPendingVisiblePreferredSize()) {
      window.refreshSizes()
      updateSceneMetrics()
    }
    updateStateFromWindow()
    renderPendingFrame()
  }

  private fun renderPendingFrame() {
    if (!canRenderFrame) {
      return
    }
    if (!renderRequested && !scene.hasInvalidations) {
      return
    }
    renderRequested = false
    peer.renderBackend.render(scene, System.nanoTime())
  }

  override fun close() {
    // GLFW can deliver callbacks while a window is being destroyed.
    peer.detachInput()
    systemThemeProvider.close()
    platformContext.destroyLifecycle()
    filePicker.close()
    scene.close()
    peer.closeNative()
  }

  private fun recreatePeer(config: WindowPeerConfig) {
    // Only the native pieces are replaced. The Compose scene survives, so UI state survives too.
    peer.close()
    actualVisible = initialNativeVisibility(config)
    val newWindow = createPlatformWindow(config, visible = actualVisible)
    val newRenderBackend = createRenderBackend(newWindow)
    platformContext.updateWindow(
      window = newWindow,
      visible = actualVisible,
      minimized = state.isMinimized,
    )
    filePicker.updateWindow(newWindow)
    scope.window.renderContext = newRenderBackend.interop
    peer = attachPeer(newWindow, newRenderBackend, config.enabled)
    lastFramebufferSize = newWindow.framebufferSize
    lastContentScale = newWindow.contentScale
    resetAppliedStateForNewPeer()
    scene.resize(newWindow.framebufferSize)
    scene.updateDensity(newWindow.contentScale)
    updateSceneMetrics()
    scope.window.info = currentInfo()
    requestRender()
  }

  private fun createPlatformWindow(
    config: WindowPeerConfig,
    visible: Boolean = config.visible,
  ): PlatformWindow =
    PlatformWindow(
      title = config.title,
      size = config.size.initialConcreteSize(),
      visible = visible,
      undecorated = config.undecorated,
      transparent = config.transparent,
      resizable = config.resizable,
      focusOnShow = config.focusOnShow,
      alwaysOnTop = config.alwaysOnTop,
      clientApi = currentWindowClientApi(),
    )

  private fun attachPeer(
    window: PlatformWindow,
    renderBackend: RenderBackendDriver,
    enabled: Boolean,
  ): WindowPeer {
    val input =
      InputDispatcher(
          window = window,
          scene = scene,
          textInput = platformContext.textInput,
          onKeyboardModifiers = platformContext::updateKeyboardModifiers,
          onKeyboardInputMode = platformContext::updateKeyboardInputMode,
          onPointerInputMode = platformContext::updatePointerInputMode,
          onPreviewKeyEvent = { onPreviewKeyEvent(it) },
          onKeyEvent = { onKeyEvent(it) },
          requestRender = ::requestRender,
        )
        .also { it.enabled = enabled }
    installWindowCallbacks(window)
    return WindowPeer(window, renderBackend, input)
  }

  private fun installWindowCallbacks(window: PlatformWindow) {
    glfwSetFramebufferSizeCallback(window.handle) { _, _, _ ->
      window.refreshSizes()
      updateSceneMetrics()
      if (!isApplyingStateSize) {
        settlePendingStateSize()
      }
      requestRender()
      renderFromGlfwCallback()
    }
    glfwSetWindowSizeCallback(window.handle) { _, _, _ ->
      window.refreshSizes()
      updateSceneMetrics()
      if (!isApplyingStateSize && !settlePendingStateSize()) {
        updateStateSizeFromWindow()
      }
      requestRender()
      renderFromGlfwCallback()
    }
    glfwSetWindowRefreshCallback(window.handle) { _ ->
      requestRender()
      renderFromGlfwCallback()
    }
    glfwSetWindowFocusCallback(window.handle) { _, focused ->
      platformContext.updateFocus(focused)
      requestRender()
    }
    glfwSetWindowCloseCallback(window.handle) { _ ->
      window.cancelCloseRequest()
      onCloseRequest()
    }
    glfwSetWindowPosCallback(window.handle) { _, _, _ ->
      window.refreshSizes()
      updateStateFromWindow()
    }
    glfwSetWindowIconifyCallback(window.handle) { _, _ ->
      window.refreshSizes()
      updateStateFromWindow()
    }
    glfwSetWindowMaximizeCallback(window.handle) { _, _ ->
      window.refreshSizes()
      updateStateFromWindow()
    }
  }

  private fun clearWindowCallbacks(window: PlatformWindow) {
    glfwSetFramebufferSizeCallback(window.handle, null)?.free()
    glfwSetWindowSizeCallback(window.handle, null)?.free()
    glfwSetWindowFocusCallback(window.handle, null)?.free()
    glfwSetWindowCloseCallback(window.handle, null)?.free()
    glfwSetWindowPosCallback(window.handle, null)?.free()
    glfwSetWindowIconifyCallback(window.handle, null)?.free()
    glfwSetWindowMaximizeCallback(window.handle, null)?.free()
    glfwSetWindowRefreshCallback(window.handle, null)?.free()
  }

  private fun applyStateToWindow(forceSize: Boolean = false) {
    val stateSize = state.size
    if (!stateSize.hasUnspecifiedDimensions() && stateSize != lastAppliedStateSize) {
      pendingVisiblePreferredSizeRequest = null
    }

    val requestedSize = resolveStateSize(stateSize)
    if (forceSize || requestedSize != lastAppliedStateSize) {
      if (forceSize || canApplyStateSizeToWindow()) {
        applyNativeStateSize(requestedSize)
      } else {
        lastAppliedStateSize = requestedSize
      }
      requestRender()
    }

    val requestedPosition = state.position
    if (requestedPosition != lastAppliedPosition) {
      applyPosition(requestedPosition)
    }

    if (state.placement != lastAppliedPlacement) {
      val requestedPlacement = state.placement
      lastAppliedPlacement = requestedPlacement
      applyPlacement(requestedPlacement)
    }

    if (state.isMinimized != lastAppliedMinimized) {
      if (state.isMinimized) {
        window.iconify()
      } else {
        window.restore()
      }
      lastAppliedMinimized = state.isMinimized
      updatePlatformLifecycleState()
    }
  }

  private fun applyPosition(position: WindowPosition) {
    if (!window.supportsWindowPosition || window.isFullscreen) {
      // TODO: Preserve or report unresolved requested positions on display servers that restrict
      // window positioning, especially Wayland.
      lastAppliedPosition = position
      return
    }

    val absolutePosition =
      when (position) {
        is WindowPosition.Absolute -> position.toPlatformPosition()
        is WindowPosition.Aligned -> position.toPlatformPosition()
        WindowPosition.PlatformDefault -> {
          lastAppliedPosition = position
          return
        }
      }
    window.setPosition(absolutePosition)
    val resolvedPosition = absolutePosition.toWindowPosition()
    lastAppliedPosition = resolvedPosition
    if (state.position != resolvedPosition) {
      state.position = resolvedPosition
    }
    requestRender()
  }

  private fun applyPlacement(placement: WindowPlacement) {
    pendingPlacement = placement
    if (placement != WindowPlacement.Floating) {
      clearPendingStateSize()
    }
    when (placement) {
      WindowPlacement.Floating -> {
        if (window.isFullscreen) {
          window.setWindowed(windowedBoundsBeforeFullscreen ?: window.currentWindowedBounds())
          windowedBoundsBeforeFullscreen = null
        } else {
          window.restore()
        }
      }
      WindowPlacement.Maximized -> {
        if (window.isFullscreen) {
          window.setWindowed(windowedBoundsBeforeFullscreen ?: window.currentWindowedBounds())
          windowedBoundsBeforeFullscreen = null
        }
        if (window.isIconified) {
          window.restore()
        }
        window.maximize()
      }
      WindowPlacement.Fullscreen -> {
        if (!window.isFullscreen) {
          windowedBoundsBeforeFullscreen = window.currentWindowedBounds()
        }
        window.setFullscreen()
      }
    }
    requestRender()
  }

  private fun resetAppliedStateForNewPeer() {
    lastAppliedStateSize = state.size.initialConcreteSize()
    clearPendingStateSize()
    lastAppliedPosition = WindowPosition.PlatformDefault
    lastAppliedPlacement = WindowPlacement.Floating
    pendingPlacement = null
    lastAppliedMinimized = false
  }

  private fun requestRender() {
    if (!renderRequested) {
      renderRequested = true
      wakeEventLoop()
    }
  }

  private val canRenderFrame: Boolean
    get() = actualVisible && window.framebufferSize.width > 0 && window.framebufferSize.height > 0

  private fun renderFromGlfwCallback() {
    if (!shouldRenderDuringBlockedEventProcessing || isRenderingFromGlfwCallback) {
      return
    }

    isRenderingFromGlfwCallback = true
    try {
      renderPendingFrame()
    } finally {
      isRenderingFromGlfwCallback = false
    }
  }

  private fun updateSceneMetrics() {
    val framebuffer = window.framebufferSize
    if (framebuffer != lastFramebufferSize) {
      lastFramebufferSize = framebuffer
      peer.renderBackend.resize(framebuffer)
      scene.resize(framebuffer)
      requestRender()
    }
    if (window.contentScale != lastContentScale) {
      lastContentScale = window.contentScale
      scene.updateDensity(window.contentScale)
      requestRender()
    }
    updatePlatformLifecycleState()
    platformContext.updateWindowInfo()
    scope.window.info = currentInfo()
  }

  private fun updateStateFromWindow() {
    if (window.supportsWindowPosition && !window.isFullscreen) {
      val position = window.windowPosition.toWindowPosition()
      if (position != state.position) {
        lastAppliedPosition = position
        state.position = position
      }
    }

    val placement =
      when {
        window.isFullscreen -> WindowPlacement.Fullscreen
        window.isMaximized -> WindowPlacement.Maximized
        else -> WindowPlacement.Floating
      }
    val expectedPlacement = pendingPlacement
    if (expectedPlacement == null || placement == expectedPlacement) {
      pendingPlacement = null
      if (placement != state.placement) {
        lastAppliedPlacement = placement
        state.placement = placement
      }
    }

    if (window.isIconified != state.isMinimized) {
      lastAppliedMinimized = window.isIconified
      state.isMinimized = window.isIconified
    }
  }

  private fun updateStateSizeFromWindow() {
    val size = currentWindowStateSize() ?: return
    if (size != state.size) {
      pendingVisiblePreferredSizeRequest = null
      clearPendingStateSize()
      lastAppliedStateSize = size
      state.size = size
      config = config.copy(size = size)
    }
  }

  private fun resolveStateSize(requestedSize: DpSize): DpSize {
    if (!requestedSize.hasUnspecifiedDimensions()) {
      return requestedSize
    }

    if (!actualVisible) {
      pendingVisiblePreferredSizeRequest = requestedSize
    } else {
      prepareSceneForPreferredSizeMeasurement()
    }

    val measuredSize = measurePreferredContentSize(requestedSize)
    val resolvedSize =
      DpSize(
        width =
          if (requestedSize.width.isSpecified) {
            requestedSize.width
          } else {
            measuredSize.width.toCeilDp(window.contentScale).coerceAtLeast(MinWindowSize)
          },
        height =
          if (requestedSize.height.isSpecified) {
            requestedSize.height
          } else {
            measuredSize.height.toCeilDp(window.contentScale).coerceAtLeast(MinWindowSize)
          },
      )

    state.size = resolvedSize
    config = config.copy(size = resolvedSize)
    return resolvedSize
  }

  private fun measurePreferredContentSize(requestedSize: DpSize): IntSize {
    val fixedWidth = requestedSize.width.toScenePixelsOrNull(window.contentScale)
    val fixedHeight = requestedSize.height.toScenePixelsOrNull(window.contentScale)
    val naturalSize =
      scene.calculatePreferredContentSize(
        fixedWidth = fixedWidth,
        fixedHeight = fixedHeight,
      )
    val preferredMaxSize = window.preferredContentSizeLimit()
    val resolvedWidth = fixedWidth ?: naturalSize.width.coerceAtMost(preferredMaxSize.width)
    val resolvedHeight = fixedHeight ?: naturalSize.height.coerceAtMost(preferredMaxSize.height)
    val needsWidthConstraint = fixedWidth != null || resolvedWidth != naturalSize.width
    val needsHeightConstraint = fixedHeight != null || resolvedHeight != naturalSize.height

    if (needsWidthConstraint || needsHeightConstraint) {
      val constrainedSize =
        scene.calculatePreferredContentSize(
          fixedWidth = if (needsWidthConstraint) resolvedWidth else null,
          fixedHeight = if (needsHeightConstraint) resolvedHeight else null,
        )
      return IntSize(
        width = fixedWidth ?: constrainedSize.width.coerceAtMost(preferredMaxSize.width),
        height = fixedHeight ?: constrainedSize.height.coerceAtMost(preferredMaxSize.height),
      )
    }

    return naturalSize
  }

  private fun applyPendingVisiblePreferredSize(): Boolean {
    val requestedSize = pendingVisiblePreferredSizeRequest ?: return false
    if (!actualVisible) {
      return false
    }

    pendingVisiblePreferredSizeRequest = null
    val resolvedSize = resolveStateSize(requestedSize)
    if (resolvedSize == lastAppliedStateSize && currentWindowStateSize() == resolvedSize) {
      return false
    }

    applyNativeStateSize(resolvedSize)
    requestRender()
    return true
  }

  private fun prepareSceneForPreferredSizeMeasurement() {
    if (currentDisplayServer() == DisplayServer.WAYLAND) {
      // A hidden Wayland window can report its final density only after it becomes visible. Drawing
      // without swapping lets Compose observe the current density before preferred sizing measures.
      peer.renderBackend.prepareForPreferredSizeMeasurement(scene, System.nanoTime())
    }
  }

  private fun canApplyStateSizeToWindow(): Boolean =
    !actualVisible || state.placement == WindowPlacement.Floating

  private fun settlePendingStateSize(): Boolean {
    // Wayland can report a compositor-adjusted size after the first buffer swap. Until GLFW reports
    // the app-requested size, keep that size authoritative instead of feeding the adjustment back
    // into WindowState.
    val pendingSize = pendingStateSize
    if (pendingSize != null) {
      if (
        state.placement != WindowPlacement.Floating || window.isMaximized || window.isFullscreen
      ) {
        clearPendingStateSize()
        return false
      }
      if (currentWindowStateSize() == pendingSize) {
        clearPendingStateSize()
      } else if (!hasReappliedPendingStateSize) {
        hasReappliedPendingStateSize = true
        applyNativeStateSize(pendingSize, resetPendingReapply = false)
      } else {
        clearPendingStateSize()
        return false
      }
      return true
    }
    return false
  }

  private fun clearPendingStateSize() {
    pendingStateSize = null
    hasReappliedPendingStateSize = false
  }

  private fun applyNativeStateSize(
    size: DpSize,
    resetPendingReapply: Boolean = true,
  ) {
    // Mark the synchronous GLFW resize as app-driven so callbacks fired inside glfwSetWindowSize do
    // not get mistaken for user/native resize.
    isApplyingStateSize = true
    try {
      window.setSize(size)
      lastAppliedStateSize = size
      pendingStateSize = size
      if (resetPendingReapply) {
        hasReappliedPendingStateSize = false
      }
    } finally {
      isApplyingStateSize = false
    }
  }

  private fun currentWindowStateSize(): DpSize? =
    if (window.logicalWindowSize.width > 0 && window.logicalWindowSize.height > 0) {
      DpSize(window.logicalWindowSize.width.dp, window.logicalWindowSize.height.dp)
    } else {
      null
    }

  private fun syncWindowVisibility() {
    val shouldBeVisible = config.visible && !state.size.hasUnspecifiedDimensions()
    if (shouldBeVisible != actualVisible) {
      window.setVisible(shouldBeVisible)
      actualVisible = shouldBeVisible
      updatePlatformLifecycleState()
      platformContext.updateWindowInfo()
      requestRender()
    }
  }

  private fun initialNativeVisibility(config: WindowPeerConfig): Boolean =
    config.visible && !config.size.hasUnspecifiedDimensions()

  private fun updatePlatformLifecycleState() {
    platformContext.updateLifecycleState(
      visible = actualVisible,
      minimized = state.isMinimized || window.isIconified,
    )
  }

  private fun currentInfo(
    window: PlatformWindow = this.window,
    renderBackend: RenderBackend = peer.renderBackend.backend,
  ): HostWindowInfo {
    val displayServer = currentDisplayServer()
    return HostWindowInfo(
      displayServer = displayServer,
      displayName = displayServer.displayConnectionName(),
      renderBackend = renderBackend,
      // Framebuffer dimensions are physical drawable pixels.
      framebufferWidth = window.framebufferSize.width,
      framebufferHeight = window.framebufferSize.height,
      // Window dimensions are cross-platform logical content-area units.
      windowWidth = window.logicalWindowSize.width,
      windowHeight = window.logicalWindowSize.height,
      contentScale = window.contentScale,
    )
  }

  private fun WindowPosition.Absolute.toPlatformPosition(): IntOffset =
    IntOffset(x.value.roundToInt(), y.value.roundToInt())

  private fun WindowPosition.Aligned.toPlatformPosition(): IntOffset {
    val workArea = window.currentMonitorWorkArea()
    val offset =
      alignment.align(
        size = window.windowSize,
        space = workArea.size,
        layoutDirection = LayoutDirection.Ltr,
      )
    return workArea.position + offset
  }

  private fun IntOffset.toWindowPosition(): WindowPosition = WindowPosition(x.dp, y.dp)

  private fun runNativeDialog(action: () -> Unit) {
    try {
      action()
    } finally {
      // Hack: NFD blocks GLFW polling, so drop stale app input queued behind the dialog.
      val wasInputEnabled = peer.input.enabled
      peer.input.enabled = false
      try {
        glfwPollEvents()
      } finally {
        peer.input.enabled = wasInputEnabled
      }
    }
  }

  private inner class WindowPeer(
    val window: PlatformWindow,
    val renderBackend: RenderBackendDriver,
    val input: InputDispatcher,
  ) : AutoCloseable {
    private var inputDetached = false
    private var nativeClosed = false

    fun detachInput() {
      if (!inputDetached) {
        clearWindowCallbacks(window)
        input.close()
        inputDetached = true
      }
    }

    fun closeNative() {
      if (!nativeClosed) {
        renderBackend.close()
        window.close()
        nativeClosed = true
      }
    }

    override fun close() {
      detachInput()
      closeNative()
    }
  }
}

private data class WindowPeerConfig(
  val title: String,
  val size: DpSize,
  val visible: Boolean,
  val undecorated: Boolean,
  val transparent: Boolean,
  val resizable: Boolean,
  val enabled: Boolean,
  val focusOnShow: Boolean,
  val alwaysOnTop: Boolean,
)

private val DefaultWindowSize = DpSize(800.dp, 600.dp)
private val MinWindowSize = 1.dp

private fun DpSize.hasUnspecifiedDimensions(): Boolean = !width.isSpecified || !height.isSpecified

private fun DpSize.initialConcreteSize(): DpSize =
  DpSize(
    width = if (width.isSpecified) width else DefaultWindowSize.width,
    height = if (height.isSpecified) height else DefaultWindowSize.height,
  )

private fun Dp.toScenePixelsOrNull(contentScale: Float): Int? =
  if (isSpecified) {
    toScenePixels(contentScale)
  } else {
    null
  }

private fun PlatformWindow.preferredContentSizeLimit(): IntSize {
  val workAreaSize = currentMonitorWorkArea().size
  return IntSize(
    width =
      minOf(
        workAreaSize.width.toScenePixels(contentScale),
        DefaultWindowSize.width.toScenePixels(contentScale),
      ),
    height =
      minOf(
        workAreaSize.height.toScenePixels(contentScale),
        DefaultWindowSize.height.toScenePixels(contentScale),
      ),
  )
}

private fun Dp.toScenePixels(contentScale: Float): Int =
  (value * contentScale).roundToInt().coerceAtLeast(1)

private fun Int.toScenePixels(contentScale: Float): Int =
  (this * contentScale).roundToInt().coerceAtLeast(1)

private fun Int.toCeilDp(contentScale: Float): Dp = ceil(this / contentScale).dp

private operator fun IntOffset.plus(offset: IntOffset): IntOffset =
  IntOffset(x + offset.x, y + offset.y)
