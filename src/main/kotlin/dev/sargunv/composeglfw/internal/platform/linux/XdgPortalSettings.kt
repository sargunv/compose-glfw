package dev.sargunv.composeglfw.internal.platform.linux

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.freedesktop.portal.Settings")
internal interface XdgPortalSettings : DBusInterface {
  @DBusMemberName("ReadOne") fun readOne(namespace: String, key: String): Variant<*>

  class SettingChanged(
    path: String,
    val namespace: String,
    val key: String,
    val value: Variant<*>,
  ) : DBusSignal(path, namespace, key, value)
}
