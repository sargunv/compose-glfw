@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

internal class GlfwPlatformContext : PlatformContext by PlatformContext.Empty() {
  val textInput: GlfwTextInputService = GlfwTextInputService()

  override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing =
    textInput.startInputMethod(request)
}
