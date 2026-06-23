package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.internal.application.ApplicationHost

/**
 * Starts a Compose GLFW application and runs until all windows are closed.
 *
 * @param configure declares the windows that belong to the application.
 */
public fun glfwApplication(configure: ApplicationScope.() -> Unit) {
  val scope = ApplicationScopeImpl()
  scope.configure()
  ApplicationHost(scope.windows).use { it.run() }
}

/** Scope used to declare the windows in a Compose GLFW application. */
public interface ApplicationScope {
  /**
   * Adds a GLFW window to the application.
   *
   * @param title title shown in the window decoration, when the display server provides one.
   * @param size initial logical size of the window content area.
   * @param options GLFW host options for this window.
   * @param content Compose content shown inside the window.
   */
  @Suppress("FunctionName")
  public fun Window(
    title: String,
    size: DpSize = DpSize(960.dp, 640.dp),
    options: WindowOptions = WindowOptions(),
    content: @Composable HostWindowScope.() -> Unit,
  )
}

internal data class WindowSpec(
  val title: String,
  val size: DpSize,
  val options: WindowOptions,
  val content: @Composable HostWindowScope.() -> Unit,
)

private class ApplicationScopeImpl : ApplicationScope {
  val windows = mutableListOf<WindowSpec>()

  override fun Window(
    title: String,
    size: DpSize,
    options: WindowOptions,
    content: @Composable HostWindowScope.() -> Unit,
  ) {
    windows += WindowSpec(title, size, options, content)
  }
}
