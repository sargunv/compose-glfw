package dev.sargunv.composeglfw

import androidx.compose.runtime.Composable
import dev.sargunv.composeglfw.internal.application.GlfwApplicationHost

public fun glfwApplication(configure: GlfwApplicationScope.() -> Unit) {
  val scope = GlfwApplicationScopeImpl()
  scope.configure()
  GlfwApplicationHost(scope.windows).use { it.run() }
}

public interface GlfwApplicationScope {
  public fun Window(
    title: String,
    size: GlfwWindowSize = GlfwWindowSize(960, 640),
    options: GlfwWindowOptions = GlfwWindowOptions(),
    content: @Composable GlfwWindowScope.() -> Unit,
  )
}

internal data class GlfwWindowSpec(
  val title: String,
  val size: GlfwWindowSize,
  val options: GlfwWindowOptions,
  val content: @Composable GlfwWindowScope.() -> Unit,
)

private class GlfwApplicationScopeImpl : GlfwApplicationScope {
  val windows = mutableListOf<GlfwWindowSpec>()

  override fun Window(
    title: String,
    size: GlfwWindowSize,
    options: GlfwWindowOptions,
    content: @Composable GlfwWindowScope.() -> Unit,
  ) {
    windows += GlfwWindowSpec(title, size, options, content)
  }
}
