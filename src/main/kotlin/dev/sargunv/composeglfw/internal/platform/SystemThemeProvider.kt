@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SystemTheme
import dev.sargunv.composeglfw.internal.platform.linux.createLinuxSystemThemeProvider

internal interface SystemThemeProvider : AutoCloseable {
  val systemTheme: SystemTheme

  override fun close() = Unit

  companion object {
    fun create(onSystemThemeChanged: (SystemTheme) -> Unit): SystemThemeProvider =
      when (hostOperatingSystem) {
        HostOperatingSystem.LINUX -> createLinuxSystemThemeProvider(onSystemThemeChanged)
        HostOperatingSystem.MACOS,
        HostOperatingSystem.OTHER -> StaticSystemThemeProvider(SystemTheme.Unknown)
      }
  }
}

private class StaticSystemThemeProvider(override val systemTheme: SystemTheme) : SystemThemeProvider
