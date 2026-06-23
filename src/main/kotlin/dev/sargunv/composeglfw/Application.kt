package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
