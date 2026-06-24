# Application Menus Plan

Compose GLFW can show Compose popup and context menus inside the scene, but it
does not expose native application or window menu bars. GLFW does not provide a
cross-platform menu abstraction, so this feature needs a host-neutral menu model
with platform adapters that use native handles exposed by GLFW.

## Goals

- Expose a Compose-friendly menu DSL for application and window commands.
- Use real AppKit menus on macOS and real Win32 menus on Windows.
- Preserve GLFW as the windowing and event-loop owner.
- Keep menu state reactive: labels, enabled state, checked state, shortcuts, and
  visibility should update from composition.
- Share the same command model across native adapters and any Linux in-window
  menu surface.

## Non-Goals

- Do not enable AWT/Swing just to reuse Compose Desktop menu plumbing.
- Do not depend on GTK or Qt for Linux menus.
- Do not promise a Linux global menu on every desktop. Linux desktops do not
  share one universally supported native menu-bar contract.
- Do not replace existing Compose scene context menus in the first pass.

## Public API Shape

Prefer a small host-owned DSL that mirrors the useful parts of Compose Desktop's
`MenuBar` API without exposing AWT types:

```kotlin
@Composable
public fun ApplicationScope.ApplicationMenuBar(content: MenuBarScope.() -> Unit)

@Composable
public fun HostWindowScope.WindowMenuBar(content: MenuBarScope.() -> Unit)
```

The first pass should support:

- Top-level menus.
- Items with labels, enabled state, mnemonic, shortcut, and callback.
- Separators.
- Checkbox/radio items with checked state.
- Nested submenus.
- Platform-standard roles for common commands such as quit, about, preferences,
  services, hide, minimize, close window, and help.

Internally, both APIs should produce the same immutable menu tree:

```kotlin
internal data class HostMenuBar(
  val menus: List<HostMenu>,
)

internal sealed interface HostMenuNode {
  val id: Long
  val label: String
  val enabled: Boolean
}

internal data class HostMenuItem(
  override val id: Long,
  override val label: String,
  override val enabled: Boolean,
  val selected: Boolean = false,
  val shortcut: HostMenuShortcut? = null,
  val role: HostMenuRole? = null,
  val onClick: () -> Unit,
) : HostMenuNode
```

`ApplicationHost` should own the application menu adapter. `WindowHost` should
own a per-window menu adapter where the platform has per-window menu bars. Menu
callbacks must dispatch back onto the existing `UiDispatcher` thread.

## Architecture

```text
ApplicationMenuBar / WindowMenuBar composition
        |
        v
HostMenuBar model
  - stable generated item ids
  - labels, mnemonics, shortcuts
  - enabled/checked/visible state
  - command callbacks
        |
        +--> MacApplicationMenuAdapter(NSApplication.mainMenu)
        +--> WindowsWindowMenuAdapter(HMENU on HWND)
        +--> LinuxDbusMenuAdapter(com.canonical.dbusmenu)
             +--> X11 AppMenu registrar by Window id
             +--> Wayland appmenu registration by wl_surface
```

The platform adapters should be replaceable when a GLFW window is recreated. The
menu model should not contain native handles. Native item ids should be
adapter-local and map back to stable `HostMenuItem.id` values.

## macOS

macOS has one process-wide menu bar owned by `NSApplication`, not a menu bar
inside each window. A GLFW Cocoa window can be used alongside AppKit menus
because GLFW exposes the `NSWindow` and `NSView` through its native access API,
and this project already has Objective-C runtime helpers.

Implementation shape:

- Add `MacApplicationMenuAdapter` under `internal/platform/macos`.
- Build an `NSMenu` tree from the active `HostMenuBar` and install it as
  `NSApplication.mainMenu`.
- Use `NSMenuItem` target/action callbacks that call through a retained
  Objective-C bridge object and dispatch the matching `HostMenuItem.onClick`.
- Map shortcuts to `keyEquivalent` and `keyEquivalentModifierMask`.
- Install standard macOS roles in the right locations:
  - App menu: About, Preferences, Services, Hide, Hide Others, Show All, Quit.
  - Window menu: Minimize, Zoom, close-window commands when present.
  - Help menu: assign the menu through `NSApplication.helpMenu`.
- Update enabled/checked state before display, either by rebuilding changed
  menus from composition or by installing an `NSMenuDelegate` for
  `menuNeedsUpdate:`.
- Treat `WindowMenuBar` as active-window state on macOS. The process menu bar
  should show the active window's menu content, with app-level menus merged in a
  deterministic order.

MVP scope:

- Application menu bar.
- Labels, separators, nested submenus.
- Enabled and checked state.
- Keyboard shortcuts using Command, Option, Control, and Shift.
- About, Preferences, Quit, and Help roles.

Deferred macOS scope:

- Services menu validation.
- Recent documents integration.
- Window-list menu integration.
- Dock menu.
- Searchable Help menu content.

## Windows

Windows menu bars are attached to individual top-level windows. GLFW exposes the
Win32 `HWND`, so the adapter can create native `HMENU` instances and attach them
to each `WindowHost`.

Implementation shape:

- Add `WindowsWindowMenuAdapter` under `internal/platform/windows`.
- Create native menus with `CreateMenu`, `CreatePopupMenu`, and `InsertMenuItem`
  or `AppendMenu`.
- Attach the top-level menu to `glfwGetWin32Window(window.handle)` with
  `SetMenu`, then call `DrawMenuBar` after updates.
- Subclass or hook the GLFW window procedure and chain to GLFW's original
  procedure. Handle:
  - `WM_COMMAND` for item activation.
  - `WM_INITMENUPOPUP` to refresh enabled/checked state before a submenu opens.
  - `WM_MENUSELECT` later if status/help text becomes part of the API.
- Keep native command ids adapter-local and map them back to stable
  `HostMenuItem.id` values.
- Destroy replaced or detached `HMENU` handles. Windows destroys menus attached
  to a destroyed window, but rebuilt unassigned menus still need explicit
  cleanup.
- Reattach the menu after peer recreation, such as when transparency changes.

MVP scope:

- Per-window `WindowMenuBar`.
- Labels, separators, nested submenus.
- Enabled and checked state.
- Mnemonics using `&` escaping.
- `WM_COMMAND` activation.

Deferred Windows scope:

- Accelerator tables for shortcuts that should fire without opening the menu.
  Compose key handling can cover command shortcuts initially, but native
  accelerator integration needs to fit GLFW's message pump ownership.
- Owner-drawn or image menu items.
- Standard system-menu customization.
- Native recent documents and jump-list integration.

## Linux

Linux should not be treated as one native menu platform. X11 and Wayland expose
native window handles, but neither gives a general desktop menubar API through
GLFW. The core Linux implementation should export the menu tree over D-Bus once
and use display-server-specific registration to associate that exported menu
with the native window or surface.

Core D-Bus export:

- Implement a `com.canonical.dbusmenu` provider with the existing D-Bus Java
  dependency.
- Use the same provider for X11 and Wayland. Menu labels, hierarchy, enabled
  state, checked state, shortcuts, and activation callbacks should not depend on
  the display server.
- Keep the provider owned by the `WindowHost`, because global-menu consumers
  need to follow the active native window.

X11 registration:

- Register the exported DBusMenu with `com.canonical.AppMenu.Registrar`.
- Use the X11 window id from `glfwGetX11Window` as the registrar key.

Wayland registration:

- Register the same exported DBusMenu through the KDE appmenu Wayland protocol.
- The Wayland protocol is only the surface mapping step. The client creates an
  appmenu object for the `wl_surface` and sets the D-Bus service name and object
  path for the already-registered DBusMenu provider.
- GLFW exposes the `wl_surface` through native access, but Compose GLFW still
  needs Wayland protocol binding code for the appmenu registration request. That
  code must not interfere with GLFW's event ownership.
- Use runtime protocol discovery. When the compositor does not advertise the
  appmenu protocol, the DBusMenu provider can still exist, but there is no
  Wayland-native surface association to publish.

Supplemental in-window surface:

- A Compose-rendered in-window menu bar can be added separately for apps that
  explicitly want a menu inside their content or titlebar.
- This should reuse the same `HostMenuBar` model, but it is not the core Linux
  native-menu implementation.

GNOME notes:

- Do not add a GNOME-specific global menu target in the first design. Current
  GNOME application patterns favor in-window primary/secondary menus rather than
  a desktop global menu bar.

MVP scope:

- DBusMenu provider model.
- X11 AppMenu registrar integration.
- KDE Wayland appmenu protocol integration.
- Desktop capability detection and diagnostics.
- Keyboard shortcut dispatch through existing Compose key handling until a Linux
  desktop export path provides native shortcut activation.

Deferred Linux scope:

- Compose-rendered in-window menu bar from the shared `HostMenuBar` model.

## Command Shortcuts

Menu shortcuts need a shared model, but the activation path can differ:

- macOS should rely on `NSMenuItem` key equivalents for native menu shortcuts.
- Windows can initially rely on Compose key handling for shortcuts and use
  native menu handling when the menu is open. Later, add accelerator tables if
  they can be integrated cleanly with GLFW's event loop.
- Linux should use Compose key handling until DBusMenu/global-menu shortcut
  activation is verified on the target desktops.

The shared shortcut model should include key, platform modifier intent, and
display text. Platform adapters should choose native labels and modifier masks.

## TODO

- [ ] Add `HostMenuBar`, `HostMenu`, `HostMenuNode`, `HostMenuShortcut`, and
      `HostMenuRole` internal models.
- [ ] Add `ApplicationMenuBar` and `WindowMenuBar` composables.
- [ ] Add menu adapter factories with no-op defaults.
- [ ] Wire application menu state through `ApplicationHost`.
- [ ] Wire window menu state through `WindowHost`.
- [ ] Add macOS Objective-C target/action bridge for menu item callbacks.
- [ ] Install `NSApplication.mainMenu` from the app/window menu model.
- [ ] Map macOS standard app/help/window roles.
- [ ] Add Windows `HMENU` builder and attach it to the GLFW `HWND`.
- [ ] Add a Win32 window-procedure hook that preserves GLFW processing.
- [ ] Handle Windows `WM_COMMAND` and `WM_INITMENUPOPUP`.
- [ ] Add Linux DBusMenu provider from the shared model.
- [ ] Register Linux DBusMenu exports with the X11 AppMenu registrar.
- [ ] Register Linux DBusMenu exports with the KDE Wayland appmenu protocol.
- [ ] Add Linux desktop capability detection and diagnostics.
- [ ] Add optional Linux in-window menu rendering from the shared model.
- [ ] Add demo coverage for nested menus, shortcuts, checked items, disabled
      items, and app/window menu precedence.
- [ ] Add manual verification notes for macOS first-thread launch, Windows
      native menu activation, Linux X11, and Linux Wayland.

## References

- GLFW native access: https://www.glfw.org/docs/latest/group__native.html
- Compose Desktop menu-bar parity reference:
  https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html
- AppKit `NSApplication.mainMenu`:
  https://developer.apple.com/documentation/appkit/nsapplication/mainmenu
- Win32 menus:
  https://learn.microsoft.com/en-us/windows/win32/menurc/about-menus
- Win32 menu creation and assignment:
  https://learn.microsoft.com/en-us/windows/win32/menurc/using-menus
- KDE Wayland appmenu protocol: https://wayland.app/protocols/kde-appmenu
- DBusMenu / AppMenu overview:
  https://hellosystem.github.io/docs/developer/menu.html
