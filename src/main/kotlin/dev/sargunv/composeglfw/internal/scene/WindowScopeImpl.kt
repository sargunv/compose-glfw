package dev.sargunv.composeglfw.internal.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sargunv.composeglfw.GpuInterop
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.HostWindowScope

internal class WindowScopeImpl(
  initialInfo: HostWindowInfo,
  override val gpu: GpuInterop,
) : HostWindowScope {
  override var windowInfo: HostWindowInfo by mutableStateOf(initialInfo)
    private set

  fun updateInfo(info: HostWindowInfo) {
    windowInfo = info
  }
}
