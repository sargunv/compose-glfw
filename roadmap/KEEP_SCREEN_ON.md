# Keep Screen On Plan

Compose already exposes `Modifier.keepScreenOn()`. In Compose GLFW it reaches
`HostPlatformContext.isKeepScreenOnEnabled`, but that property currently
delegates to `PlatformContext.Empty()`, so the modifier has no host effect.

## Goals

- Honor `Modifier.keepScreenOn()` for Compose content hosted in GLFW windows.
- Keep the display awake while any Compose GLFW window has active keep-screen-on
  content.
- Release the native assertion or inhibitor as soon as all active requests are
  gone or the application closes.
- Use the normal platform power-management APIs directly; do not route through
  AWT, Swing, or a helper process.
- Keep the public surface unchanged for the first pass.

## Non-goals

- Do not override explicit user actions such as lock, sleep, lid close, power
  button, low-battery sleep, or administrator policy.
- Do not wake an already-sleeping display just because content enters the
  composition.
- Do not add a long-lived system-awake API unless a separate use case needs it.
- Do not add toolkit dependencies such as GTK or Qt on Linux.

## Compose Hook

`Modifier.keepScreenOn()` increments a per-owner count when the modifier node is
attached and decrements it on detach. Compose then sets
`PlatformContext.isKeepScreenOnEnabled` based on whether that owner has at least
one active request.

Compose GLFW should bridge that property directly in `HostPlatformContext`:

```kotlin
override var isKeepScreenOnEnabled: Boolean
  get() = keepScreenOnEnabled
  set(value) {
    if (value != keepScreenOnEnabled) {
      keepScreenOnEnabled = value
      keepScreenOn.setEnabled(value)
    }
  }
```

`keepScreenOn` can be a tiny platform bridge created from `hostOperatingSystem`.
It only needs to hold the current native assertion/request handle for this
platform context and expose `setEnabled(Boolean)` plus `close()`. Compose
already owns the modifier reference count for the scene, so Compose GLFW does
not need an application-level coordinator or a second owner-tracking layer.

Lifecycle rules:

- Create the platform bridge inside `HostPlatformContext`.
- Only call the native backend when the value changes.
- In `destroyLifecycle()` or any future `HostPlatformContext.close()`, call
  `setEnabled(false)` and close the bridge.
- Treat native setup failure as best-effort: keep the property state coherent,
  log or silently ignore the unsupported backend, and retry on the next false to
  true transition only if the backend is restartable.

## Platform Mapping

| Platform | First implementation                                                                                           | Release path                                                   | Notes                                                                                                                   |
| -------- | -------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| macOS    | IOKit power assertion: `kIOPMAssertionTypePreventUserIdleDisplaySleep` at `kIOPMAssertionLevelOn`              | `IOPMAssertionRelease(assertionId)`                            | Prevents idle display sleep and idle system sleep while active; does not wake the display or override lid/user sleep.   |
| Windows  | `PowerCreateRequest` plus `PowerSetRequest` for `PowerRequestDisplayRequired` and `PowerRequestSystemRequired` | `PowerClearRequest` for both request types, then `CloseHandle` | The system request is needed with the display request so the system does not idle-sleep underneath the display request. |
| Linux    | XDG desktop portal `org.freedesktop.portal.Inhibit.Inhibit` with flag `8` (`Idle`)                             | `org.freedesktop.portal.Request.Close` on the returned handle  | Modern Wayland, X11, and sandbox-friendly desktop path.                                                                 |

## macOS Plan

Add a macOS keep-screen-on bridge under `internal/platform/macos`.

Implementation shape:

- Load `IOKit.framework` with the existing native dynamic-loader pattern or Java
  FFM.
- Create a retained `CFString` reason such as
  `Compose GLFW keepScreenOn content`.
- Call `IOPMAssertionCreateWithName` or `IOPMAssertionCreateWithDescription`
  with assertion type `PreventUserIdleDisplaySleep` and level `255`.
- Store the returned `IOPMAssertionID`.
- Release the assertion with `IOPMAssertionRelease` when `setEnabled(false)` is
  called or the bridge is closed.
- Prefer `kIOPMAssertionTypePreventUserIdleDisplaySleep`; do not use deprecated
  `kIOPMAssertionTypeNoDisplaySleep`.

Manual verification:

- Add a demo panel with a `Modifier.keepScreenOn()` toggle.
- Run the demo on macOS and inspect `pmset -g assertions` while toggling.
- Confirm the assertion disappears after disabling the toggle and after window
  close.

## Windows Plan

Add a Windows keep-screen-on bridge under `internal/platform/windows`.

Implementation shape:

- Use Java FFM to call Kernel32 power-request functions.
- Create one power request object with a reason context.
- On activation, call `PowerSetRequest` for both:
  - `PowerRequestDisplayRequired`
  - `PowerRequestSystemRequired`
- On deactivation, call `PowerClearRequest` for both, then close the handle when
  the bridge closes.
- If the structured `REASON_CONTEXT` path becomes too much native plumbing for
  the first pass, use
  `SetThreadExecutionState(ES_CONTINUOUS |
  ES_DISPLAY_REQUIRED | ES_SYSTEM_REQUIRED)`
  as a temporary implementation and clear it with
  `SetThreadExecutionState(ES_CONTINUOUS)`. The power-request path should remain
  the target because it gives Windows a named request object.

Manual verification:

- Run the demo on Windows and inspect `powercfg /requests`.
- Confirm both display and system requests are present only while enabled.
- Confirm requests clear after disabling the toggle and after window close.

## Linux Plan

Add a Linux keep-screen-on bridge under `internal/platform/linux`, reusing the
existing `dbus-java` dependency.

Portal path:

- Connect to the session bus.
- Call `org.freedesktop.portal.Inhibit.Inhibit` on
  `/org/freedesktop/portal/desktop`.
- Pass flag `8` for idle inhibition.
- Include a user-visible `reason`.
- Pass a parent-window identifier when available:
  - X11 can use `x11:<XID>` from `glfwGetX11Window`.
  - Wayland needs an `xdg_foreign` exported surface handle; until that exists,
    pass an empty string.
- Store the returned request object path and call
  `org.freedesktop.portal.Request.Close` to release it.

If the portal bus object or method is unavailable, treat keep-screen-on as
unsupported for that session and leave the bridge as a no-op. Do not add
`org.freedesktop.ScreenSaver` or X11-only `XScreenSaverSuspend` fallbacks in the
first pass; they add extra contracts without helping the modern portal-based
target.

Manual verification:

- On GNOME/KDE, check that the inhibitor appears in the shell's power/session
  tooling where available.
- On wlroots-based Wayland compositors, verify with the compositor's idle daemon
  or lock timer rather than assuming portal support is wired correctly.
- On X11, verify the portal path.

## Open Questions

- Application identity: portals want an application name or app id. Compose GLFW
  does not currently have an application metadata API. The first pass can use a
  stable library string, but a later `ApplicationOptions` or `WindowOptions`
  field may be useful.
- Failure visibility: decide whether unsupported native keep-screen-on should
  stay silent like many Compose platform hooks or expose a debug log hook.
- Scope: `Modifier.keepScreenOn()` is display-oriented. If future callers need
  CPU/background work to continue with the screen off, that should be a separate
  API rather than overloading this modifier.

## TODO

- [ ] Add a tiny platform keep-screen-on bridge with a no-op default.
- [ ] Wire `HostPlatformContext.isKeepScreenOnEnabled` to that bridge.
- [ ] Ensure `HostPlatformContext` disables and closes the bridge on destroy.
- [ ] Add macOS IOKit assertion backend.
- [ ] Add Windows Kernel32 power-request backend.
- [ ] Add Linux XDG portal inhibitor backend.
- [ ] Add a demo toggle that applies `Modifier.keepScreenOn()`.
- [ ] Manually verify macOS with `pmset -g assertions`.
- [ ] Manually verify Windows with `powercfg /requests`.
- [ ] Manually verify Linux on at least one Wayland desktop and one X11 session.

## References

- Compose `Modifier.keepScreenOn()` source:
  `androidx.compose.ui.KeepScreenOn.kt` in `ui-desktop` sources.
- Windows `PowerCreateRequest`:
  https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-powercreaterequest
- Windows `PowerSetRequest`:
  https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-powersetrequest
- Windows `PowerClearRequest`:
  https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-powerclearrequest
- Windows `SetThreadExecutionState`:
  https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-setthreadexecutionstate
- Apple `IOPMAssertionCreateWithName`:
  https://developer.apple.com/documentation/iokit/1557134-iopmassertioncreatewithname
- Apple IOPM assertion constants and release function:
  https://github.com/opensource-apple/IOKitUser/blob/master/pwr_mgt.subproj/IOPMLib.h
- XDG desktop portal `Inhibit`:
  https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.Inhibit.html
- XDG desktop portal window identifiers:
  https://flatpak.github.io/xdg-desktop-portal/docs/window-identifiers.html
