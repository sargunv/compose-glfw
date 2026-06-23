package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.KeyEvent
import dev.sargunv.composeglfw.internal.application.ApplicationHost
import dev.sargunv.composeglfw.internal.application.ApplicationScopeImpl
import dev.sargunv.composeglfw.internal.application.WindowRequest

/**
 * Starts a Compose GLFW application and runs until the application exits.
 *
 * Windows are declared with [Window] inside the application composition. When a [Window] enters the
 * composition, a native GLFW window is created; when it leaves, that window is closed.
 *
 * @param content application-level composition.
 */
public fun glfwApplication(content: @Composable ApplicationScope.() -> Unit) {
  ApplicationHost(content).use { it.run() }
}

/** Scope used by [glfwApplication]. */
@Stable
public interface ApplicationScope {
  /** Closes all windows created by the application and stops application-level effects. */
  public fun exitApplication()
}

/**
 * Composes a GLFW window in the current application composition.
 *
 * @param onCloseRequest called when the user requests that the native window should close. The
 *   application decides whether to remove this [Window] from the composition or exit entirely.
 * @param state state used to control and observe runtime window attributes.
 * @param visible whether the native window is visible.
 * @param title title shown in the window decoration, when the display server provides one.
 * @param undecorated whether to request a window without native decorations.
 * @param transparent whether the window content framebuffer should include alpha. This is a native
 *   window creation hint and requires recreation to change at runtime.
 * @param resizable whether the user can resize the window.
 * @param enabled whether the window reacts to input events.
 * @param focusOnShow whether the window should receive focus when shown.
 * @param alwaysOnTop whether the window stays above other normal windows.
 * @param onPreviewKeyEvent key event callback invoked before the event is sent to Compose content.
 * @param onKeyEvent key event callback invoked if Compose content does not consume the event.
 * @param options GLFW host options for this window.
 * @param content Compose content shown inside the window.
 */
@Composable
@ComposableOpenTarget(-1)
@Suppress("FunctionName")
public fun ApplicationScope.Window(
  onCloseRequest: () -> Unit,
  state: WindowState = rememberWindowState(),
  visible: Boolean = true,
  title: String = "Untitled",
  undecorated: Boolean = false,
  transparent: Boolean = false,
  resizable: Boolean = true,
  enabled: Boolean = true,
  focusOnShow: Boolean = true,
  alwaysOnTop: Boolean = false,
  onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
  onKeyEvent: (KeyEvent) -> Boolean = { false },
  options: WindowOptions = WindowOptions(),
  content: @Composable HostWindowScope.() -> Unit,
) {
  val application =
    this as? ApplicationScopeImpl ?: error("Window must be used inside glfwApplication")
  val currentOnCloseRequest = rememberUpdatedState(onCloseRequest)
  val currentOnPreviewKeyEvent = rememberUpdatedState(onPreviewKeyEvent)
  val currentOnKeyEvent = rememberUpdatedState(onKeyEvent)
  val currentContent = rememberUpdatedState(content)
  val host =
    remember(application) {
      application.createWindow(
        WindowRequest(
          title = title,
          state = state,
          visible = visible,
          undecorated = undecorated,
          transparent = transparent,
          resizable = resizable,
          enabled = enabled,
          focusOnShow = focusOnShow,
          alwaysOnTop = alwaysOnTop,
          options = options,
          onCloseRequest = { currentOnCloseRequest.value() },
          onPreviewKeyEvent = { currentOnPreviewKeyEvent.value(it) },
          onKeyEvent = { currentOnKeyEvent.value(it) },
          content = { currentContent.value(this) },
        )
      )
    }

  SideEffect {
    host.update(
      title = title,
      state = state,
      visible = visible,
      undecorated = undecorated,
      transparent = transparent,
      resizable = resizable,
      enabled = enabled,
      focusOnShow = focusOnShow,
      alwaysOnTop = alwaysOnTop,
      options = options,
    )
  }

  DisposableEffect(host) {
    onDispose { application.disposeWindow(host) }
  }
}
