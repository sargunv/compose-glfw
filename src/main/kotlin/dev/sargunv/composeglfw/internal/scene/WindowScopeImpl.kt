package dev.sargunv.composeglfw.internal.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sargunv.composeglfw.GpuInterop
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.HostWindowScope

internal class WindowScopeImpl(
  initialInfo: HostWindowInfo,
  initialGpu: GpuInterop,
) : HostWindowScope {
  override var windowInfo: HostWindowInfo by mutableStateOf(initialInfo)

  override var gpu: GpuInterop by mutableStateOf(initialGpu)
}
