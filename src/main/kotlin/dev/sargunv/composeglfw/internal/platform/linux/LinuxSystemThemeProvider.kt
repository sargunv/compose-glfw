@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform.linux

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SystemTheme
import dev.sargunv.composeglfw.internal.platform.SystemThemeProvider
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.matchrules.DBusMatchRuleBuilder
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

internal fun createLinuxSystemThemeProvider(
  onSystemThemeChanged: (SystemTheme) -> Unit
): SystemThemeProvider = XdgPortalSystemThemeProvider(onSystemThemeChanged)

private class XdgPortalSystemThemeProvider(
  private val onSystemThemeChanged: (SystemTheme) -> Unit
) : SystemThemeProvider {
  private val connection: DBusConnection?
  private val signalSubscription: AutoCloseable?
  private var currentSystemTheme: SystemTheme = SystemTheme.Unknown

  override val systemTheme: SystemTheme
    get() = currentSystemTheme

  init {
    val initialized = runCatching {
      val dbusConnection = DBusConnectionBuilder.forSessionBus().withShared(false).build()
      val settings =
        dbusConnection.getRemoteObject(
          PortalBusName,
          PortalObjectPath,
          XdgPortalSettings::class.java,
        )

      currentSystemTheme = settings.readSystemTheme()
      dbusConnection to dbusConnection.subscribeToColorSchemeChanges(settings)
    }

    connection = initialized.getOrNull()?.first
    signalSubscription = initialized.getOrNull()?.second
  }

  override fun close() {
    runCatching { signalSubscription?.close() }
    runCatching { connection?.disconnect() }
  }

  private fun XdgPortalSettings.readSystemTheme(): SystemTheme =
    readOne(AppearanceNamespace, ColorSchemeKey).toSystemTheme()

  private fun DBusConnection.subscribeToColorSchemeChanges(
    settings: XdgPortalSettings
  ): AutoCloseable {
    val rule =
      DBusMatchRuleBuilder.create()
        .withType(XdgPortalSettings.SettingChanged::class.java)
        .withPath(settings.objectPath)
        .build()

    return addSigHandler(
      rule,
      DBusSigHandler<XdgPortalSettings.SettingChanged> { signal ->
        if (signal.namespace == AppearanceNamespace && signal.key == ColorSchemeKey) {
          updateSystemTheme(signal.value.toSystemTheme())
        }
      },
    )
  }

  private fun updateSystemTheme(systemTheme: SystemTheme) {
    if (systemTheme != currentSystemTheme) {
      currentSystemTheme = systemTheme
      onSystemThemeChanged(systemTheme)
    }
  }
}

private fun Variant<*>.toSystemTheme(): SystemTheme =
  when ((value as? UInt32)?.toInt()) {
    1 -> SystemTheme.Dark
    2 -> SystemTheme.Light
    else -> SystemTheme.Unknown
  }

private const val PortalBusName = "org.freedesktop.portal.Desktop"
private const val PortalObjectPath = "/org/freedesktop/portal/desktop"
private const val AppearanceNamespace = "org.freedesktop.appearance"
private const val ColorSchemeKey = "color-scheme"
