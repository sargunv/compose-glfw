package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import dev.sargunv.composeglfw.internal.application.ApplicationHost

public fun glfwApplication(configure: ApplicationScope.() -> Unit) {
  val scope = ApplicationScopeImpl()
  scope.configure()
  ApplicationHost(scope.windows).use { it.run() }
}

public interface ApplicationScope {
  @Suppress("FunctionName")
  public fun Window(
    title: String,
    size: WindowSize = WindowSize(960, 640),
    options: WindowOptions = WindowOptions(),
    content: @Composable HostWindowScope.() -> Unit,
  )
}

internal data class WindowSpec(
  val title: String,
  val size: WindowSize,
  val options: WindowOptions,
  val content: @Composable HostWindowScope.() -> Unit,
)

private class ApplicationScopeImpl : ApplicationScope {
  val windows = mutableListOf<WindowSpec>()

  override fun Window(
    title: String,
    size: WindowSize,
    options: WindowOptions,
    content: @Composable HostWindowScope.() -> Unit,
  ) {
    windows += WindowSpec(title, size, options, content)
  }
}
