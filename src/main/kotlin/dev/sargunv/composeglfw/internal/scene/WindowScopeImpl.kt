package dev.sargunv.composeglfw.internal.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sargunv.composeglfw.FilePicker
import dev.sargunv.composeglfw.HostWindow
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.HostWindowScope
import dev.sargunv.composeglfw.RenderContext

internal class WindowScopeImpl(
  initialInfo: HostWindowInfo,
  initialRenderContext: RenderContext,
  initialFilePicker: FilePicker,
) : HostWindowScope {
  override val window: WindowImpl = WindowImpl(initialInfo, initialRenderContext, initialFilePicker)
}

internal class WindowImpl(
  initialInfo: HostWindowInfo,
  initialRenderContext: RenderContext,
  initialFilePicker: FilePicker,
) : HostWindow {
  override var info: HostWindowInfo by mutableStateOf(initialInfo)

  override var renderContext: RenderContext by mutableStateOf(initialRenderContext)

  override var filePicker: FilePicker by mutableStateOf(initialFilePicker)
}
