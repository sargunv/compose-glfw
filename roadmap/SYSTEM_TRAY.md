# System Tray Plan

Compose GLFW does not get system tray support from GLFW. The tray needs a small
Compose-facing API backed by native platform adapters, with notifications
handled as a related but separate platform concern where the tray protocol does
not provide them.

## Goals

- Support a Compose Desktop-like `Tray` API for icon, tooltip, action, menu, and
  notification state.
- Preserve GLFW as the windowing and event-loop owner.
- Keep AWT/Swing out of the implementation path.
- Share menu modeling with the native menu roadmap so tray menus and top-level
  menus do not grow two independent abstractions.
- Use platform APIs directly enough to support icon updates, lifecycle cleanup,
  click/action routing, and native-looking context menus.

## Non-Goals

- Do not use `java.awt.SystemTray` or `java.awt.TrayIcon`; that would
  reintroduce the desktop host stack this project is replacing.
- Do not try to draw a custom tray window. The operating system or desktop shell
  owns tray/menu-bar presentation.
- Do not implement the older XEmbed tray protocol. StatusNotifierItem is the
  Linux target for both X11 and Wayland sessions.
- Do not block the initial tray icon/menu work on full notification-center or
  toast parity.

## Public API Shape

Mirror Compose Desktop where practical, but keep the symbols in the
`dev.sargunv.composeglfw` API namespace:

```kotlin
public val isTraySupported: Boolean

@Composable
public fun ApplicationScope.Tray(
  icon: Painter,
  state: TrayState = rememberTrayState(),
  tooltip: String? = null,
  onAction: () -> Unit = {},
  menu: @Composable @MenuComposable MenuScope.() -> Unit = {},
)

@Composable
public fun rememberTrayState(): TrayState

public class TrayState {
  public fun sendNotification(notification: Notification)
}
```

The exact public types should be decided with the native-menu API, because tray
menus need the same item, submenu, separator, check, radio, enabled, icon, and
shortcut model as app/window menus. The first implementation can support a
subset of menu fields when the platform adapter cannot express every property.

## Host Model

Add an internal platform-neutral adapter boundary:

```kotlin
internal interface SystemTrayAdapter : AutoCloseable {
  val isSupported: Boolean
  fun update(item: HostTrayItem)
  fun showNotification(notification: HostNotification)
}

internal data class HostTrayItem(
  val id: String,
  val icon: HostTrayIcon,
  val tooltip: String?,
  val menu: HostMenu?,
  val onAction: () -> Unit,
)
```

The Compose-facing `Tray` composable should render the `Painter` into
resolution-specific bitmap data, compose the menu into the shared host menu
model, then call `SystemTrayAdapter.update`. Adapter instances belong to
`ApplicationHost`, not to an individual `WindowHost`, because a tray icon can
outlive all windows.

`TrayState.sendNotification` should remain best-effort. It should route through
the active tray adapter when present, but each platform may delegate to a
different notification API.

## Platform APIs

| Platform | Primary tray API                            | Notification API                                       | First-pass support                        |
| -------- | ------------------------------------------- | ------------------------------------------------------ | ----------------------------------------- |
| macOS    | AppKit `NSStatusBar` / `NSStatusItem`       | `UNUserNotificationCenter`                             | icon, tooltip, action, `NSMenu`           |
| Windows  | Shell `Shell_NotifyIcon` / `NOTIFYICONDATA` | `Shell_NotifyIcon` balloons first; modern toasts later | icon, tooltip, action, popup menu         |
| Linux    | StatusNotifierItem over D-Bus               | `org.freedesktop.Notifications` or portal notification | SNI icon, tooltip, activate, context menu |

## macOS

Use AppKit `NSStatusBar` and `NSStatusItem`:

- Create an item from the system status bar using `statusItemWithLength:`.
- Retain the `NSStatusItem` for the lifetime of the tray entry; releasing it
  removes the item.
- Use the item's `button` for image, tooltip, target/action, and alternate click
  handling.
- Assign an `NSMenu` to `NSStatusItem.menu` for the normal native menu path.
- Use template-style monochrome icon data when the API allows it, so the system
  can adapt the item for light/dark menu bar appearances.
- Run all AppKit interaction on the main thread. `ApplicationRuntime` already
  enforces main-thread startup on macOS, and `MacObjectiveC` is the right place
  to extend Objective-C helpers.

Implementation notes:

- The existing `MacObjectiveC` helper can send messages and load AppKit, but
  tray actions probably need additional Objective-C runtime support for a small
  target class, method implementation callbacks, or block-backed actions.
- Native `NSMenu` construction should share code with native-menu support.
- `UNUserNotificationCenter` is the modern notification path. Treat it as a
  second step because it has authorization and app identity requirements that
  are separate from status item display.

## Windows

Use Shell notification area APIs:

- Get or create an HWND owned by the application. The current GLFW windows have
  HWNDs on Win32, but a tray entry should not disappear just because a Compose
  window closes; a message-only or hidden owner window may be cleaner.
- Fill `NOTIFYICONDATA` with an icon handle, tooltip, callback message, and a
  stable GUID where possible.
- Call `Shell_NotifyIcon` with `NIM_ADD`, then call it again with
  `NIM_SETVERSION` and `NOTIFYICON_VERSION_4`.
- Update with `NIM_MODIFY` and remove with `NIM_DELETE`.
- Handle callback messages for activation, context menu, keyboard selection,
  balloon events, and rich-popup open/close if those are exposed later.
- Show a native menu with the Win32 popup-menu APIs at the click coordinates and
  call `NIM_SETFOCUS` after the menu interaction completes.

Implementation notes:

- LWJGL already provides Win32 helpers for some APIs, but Shell32 coverage and
  the exact `NOTIFYICONDATA` structure support need verification before coding.
  If LWJGL does not expose enough, add a small direct JNI/JDK FFM layer rather
  than JNA.
- Shell notification-area balloon notifications can satisfy the first
  `TrayState.sendNotification` pass. Windows App SDK toast notifications are a
  broader packaging/app identity feature and should be separate from tray MVP.

## Linux

Use StatusNotifierItem over the session bus as the Linux tray target. This is
not tied to GLFW's X11 or Wayland backend; the desktop shell watches D-Bus and
renders the item in whatever panel/status area it owns.

- Export an application-owned D-Bus service such as
  `org.freedesktop.StatusNotifierItem-PID-ID`.
- Export a `/StatusNotifierItem` object implementing the SNI properties,
  methods, and signals needed by a host.
- Register the item with `org.freedesktop.StatusNotifierWatcher` using
  `RegisterStatusNotifierItem`.
- Provide `Category`, `Id`, `Title`, `Status`, icon data or icon name, tooltip,
  and activation/context-menu methods.
- Emit `NewIcon`, `NewToolTip`, `NewStatus`, and related signals when Compose
  state changes.
- Prefer icon names only when the app has installed themed icons; otherwise
  provide pixel data so an unpackaged demo app can work.

Implementation notes:

- The project already depends on `dbus-java`; reuse it instead of adding a GTK,
  Qt, or libappindicator dependency.
- The freedesktop spec uses `org.freedesktop.StatusNotifierItem`, but common
  implementations also involve the older `org.kde.StatusNotifierItem` naming.
  Verify real desktops before locking the exported interface names.
- SNI context menus commonly use the D-Bus menu protocol. If menu support is too
  large for the first pass, ship activate-only tray support first and gate menus
  behind the native-menu work.
- Notifications should use `org.freedesktop.Notifications` or the XDG desktop
  portal notification API, not SNI itself.

## TODO

- [ ] Define the public `Tray`, `TrayState`, and `Notification` API in this
      project's namespace.
- [ ] Define the shared host menu model needed by tray menus and native menus.
- [ ] Add `SystemTrayAdapter` and a no-op/default adapter.
- [ ] Add application-level tray ownership to `ApplicationHost`.
- [ ] Add `Painter` to tray icon bitmap rendering with multi-size/high-DPI
      outputs.
- [ ] Add macOS `NSStatusItem` adapter with icon, tooltip, action, and cleanup.
- [ ] Add Objective-C runtime callback support for macOS action/menu handlers.
- [ ] Add Windows `Shell_NotifyIcon` adapter with version 4 callback handling.
- [ ] Add Linux SNI D-Bus adapter using `dbus-java`.
- [ ] Add a demo tray entry with notification send, icon update, and exit item.
- [ ] Manually verify macOS menu bar, Windows notification area, KDE Plasma SNI,
      GNOME with AppIndicator/SNI extension, and an environment with no tray
      host.

## Sources

- Apple `NSStatusBar`:
  https://developer.apple.com/documentation/appkit/nsstatusbar
- Apple `NSStatusItem`:
  https://developer.apple.com/documentation/appkit/nsstatusitem
- Apple `UNUserNotificationCenter`:
  https://developer.apple.com/documentation/usernotifications/unusernotificationcenter
- Microsoft notification area overview:
  https://learn.microsoft.com/en-us/windows/win32/shell/notification-area
- Microsoft `Shell_NotifyIcon`:
  https://learn.microsoft.com/en-us/windows/win32/api/shellapi/nf-shellapi-shell_notifyicona
- Freedesktop StatusNotifierItem:
  https://specifications.freedesktop.org/status-notifier-item/latest-single/
- Freedesktop Desktop Notifications:
  https://specifications.freedesktop.org/notification/1.3/
- Compose Multiplatform desktop-only API:
  https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html
