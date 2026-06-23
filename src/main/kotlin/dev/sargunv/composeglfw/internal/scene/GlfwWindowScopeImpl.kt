package dev.sargunv.composeglfw.internal.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sargunv.composeglfw.GlfwGpuInterop
import dev.sargunv.composeglfw.GlfwWindowInfo
import dev.sargunv.composeglfw.GlfwWindowScope

internal class GlfwWindowScopeImpl(
  initialInfo: GlfwWindowInfo,
  override val gpu: GlfwGpuInterop,
) : GlfwWindowScope {
  override var windowInfo: GlfwWindowInfo by mutableStateOf(initialInfo)
    private set

  fun updateInfo(info: GlfwWindowInfo) {
    windowInfo = info
  }
}
