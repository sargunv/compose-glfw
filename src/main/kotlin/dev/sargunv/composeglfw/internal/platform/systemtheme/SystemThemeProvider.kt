@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform.systemtheme

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SystemTheme
import dev.sargunv.composeglfw.internal.platform.linux.createLinuxSystemThemeProvider

internal interface SystemThemeProvider : AutoCloseable {
  val systemTheme: SystemTheme

  override fun close() = Unit

  companion object {
    fun create(onSystemThemeChanged: (SystemTheme) -> Unit): SystemThemeProvider =
      if (System.getProperty("os.name").equals("Linux", ignoreCase = true)) {
        createLinuxSystemThemeProvider(onSystemThemeChanged)
      } else {
        StaticSystemThemeProvider(SystemTheme.Unknown)
      }
  }
}

private class StaticSystemThemeProvider(override val systemTheme: SystemTheme) : SystemThemeProvider
