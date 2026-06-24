# Draggable Window Areas

This note sketches support for custom title bars and other draggable regions in
undecorated Compose GLFW windows.

The important rule is that a draggable window area should start the platform's
native interactive move operation. It should not repeatedly set the window
position from pointer deltas except as a last-resort fallback on platforms where
that is already the native toolkit behavior. Native moves preserve compositor or
window-manager behavior such as snapping, tiling, multi-monitor handoff,
constraints, security policy, and cursor/device grabs.

## Goals

- Let Compose content mark a region as a window drag handle.
- Match Compose Desktop's public shape where practical.
- Use compositor/window-manager-owned move operations on every backend.
- Make Wayland work without relying on global window coordinates.
- Keep the implementation useful for future custom resize borders and window
  menus.

## Non-goals

- Do not implement Wayland window positioning. Wayland intentionally does not
  expose arbitrary global toplevel placement to normal clients.
- Do not depend on AWT, Swing, or JetBrains Runtime APIs. Compose Desktop's
  implementation is a reference for the public API shape, not a usable backend
  for this host.
- Do not require a toolkit such as GTK or Qt just to move a GLFW window.

## Compose API Shape

Compose Desktop exposes:

```kotlin
@Composable
fun WindowScope.WindowDraggableArea(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {},
)
```

For Compose GLFW, mirror that in this package:

```kotlin
@Composable
public fun HostWindowScope.WindowDraggableArea(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {},
)
```

The modifier should listen for the first primary pointer down inside the region
and immediately ask the host window to begin a native move. It should not track
subsequent pointer movement itself when the native request succeeds, because the
platform/compositor owns the grab after that point.

Sketch:

```kotlin
@Composable
public fun HostWindowScope.WindowDraggableArea(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {},
) {
  Box(
    modifier = modifier.pointerInput(window) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        window.startInteractiveMove(down)
      }
    },
    propagateMinConstraints = true,
    content = { content() },
  )
}
```

The actual host call should receive enough event context to bind the request to
the input event:

```kotlin
internal interface WindowDragController : AutoCloseable {
  fun startMove(trigger: WindowDragTrigger): Boolean
}

internal data class WindowDragTrigger(
  val pointerId: Long,
  val positionInWindow: IntOffset,
  val button: PointerButton,
  val nativeSerial: Long?,
  val nativeTimestamp: Long?,
)
```

`nativeSerial` is primarily for Wayland. `nativeTimestamp` is useful for X11 and
some native event APIs. Backends that do not need a field can ignore it.

## Platform Mapping

| Platform      | Native operation                                                         | Required context                                              | Notes                                                                            |
| ------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| macOS/Cocoa   | `NSWindow.performDrag(with:)`                                            | original mouse-down `NSEvent` or an equivalent event path     | Best first target on the user's current machine.                                 |
| Windows/Win32 | `WM_NCHITTEST` returns `HTCAPTION`, or send native caption move messages | `HWND`, screen point/button event                             | Hit testing is the most native path and preserves snap/layout behavior.          |
| X11           | `_NET_WM_MOVERESIZE` with direction `MOVE`                               | X11 `Window`, root x/y, button, source indication, event time | Lets the window manager control the operation.                                   |
| Wayland       | `xdg_toplevel.move(seat, serial)`                                        | `xdg_toplevel`, `wl_seat`, valid button/touch-down serial     | This is the canonical path. The compositor may ignore stale or invalid requests. |

## macOS Plan

Use GLFW native access to get the `NSWindow`:

- `glfwGetCocoaWindow(window.handle)` returns the `NSWindow`.
- `glfwGetCocoaView(window.handle)` returns the `NSView`.

Implementation options:

- Preferred: call `performDrag(with:)` from the original `mouseDown:` handling
  path. This means a small Cocoa hook/subclass on the GLFW content view or a
  local event monitor that can capture the `NSEvent` before Compose consumes it.
- Possible but weaker: reconstruct enough `NSEvent` data and call
  `performDrag(with:)`. This needs testing; AppKit documentation describes the
  operation in terms of the original mouse-down event.
- Broad option for whole-window dragging: `isMovableByWindowBackground = true`.
  This is too coarse for arbitrary Compose regions, but can be exposed later as
  a window option if useful.

Native titlebar customization on macOS is a separate concern. For integrated
custom title bars, AppKit supports full-size content views and transparent
titlebars, but a truly undecorated GLFW window plus Compose-drawn controls still
needs explicit close/minimize/zoom actions.

MVP:

- Add `MacWindowDragController`.
- Install an Objective-C/Cocoa event hook on the GLFW view.
- On left mouse down, let Compose hit-test the draggable region.
- If accepted, call `performDrag(with:)` with that event.
- Verify native Spaces/full-screen behavior, multi-monitor moves, and double
  click interactions separately.

## Windows Plan

Use GLFW native access to get the `HWND`:

- `glfwGetWin32Window(window.handle)`.

The best implementation is nonclient hit testing:

- Subclass the window procedure.
- On `WM_NCHITTEST`, convert the screen point to Compose/window coordinates.
- If the point falls in a registered draggable region, return `HTCAPTION`.
- Let `DefWindowProc` handle the rest.

This aligns with Win32's model: the OS asks which nonclient region a point lies
in, and returning `HTCAPTION` makes that client-drawn area behave like a title
bar. It preserves snap assist, drag-to-maximize, and other window-manager
behavior better than manually handling mouse motion.

Fallback:

- On pointer down, call `ReleaseCapture()` and send `WM_NCLBUTTONDOWN` with
  `HTCAPTION`. This common technique may be useful if hit-test integration is
  awkward with GLFW's callback flow, but hit testing should be tried first.

Resize support should use the same hit-test hook later by returning `HTLEFT`,
`HTTOPRIGHT`, etc. It should not copy Compose Desktop's current manual resize
logic, because that loses native resize behavior.

## X11 Plan

Use GLFW native access:

- `glfwGetX11Display()` returns `Display*`.
- `glfwGetX11Window(window.handle)` returns the X11 `Window`.

On primary button down in a draggable region:

- Release client grabs if any.
- Send a `_NET_WM_MOVERESIZE` client message to the root window.
- Set `data.l[0]`/`data.l[1]` to the root pointer coordinates.
- Set `data.l[2]` to `_NET_WM_MOVERESIZE_MOVE`.
- Set `data.l[3]` to the button.
- Set `data.l[4]` to application source indication.

This is the EWMH path for client-defined move grips while letting the window
manager own the operation. It works better than repeated `glfwSetWindowPos`
calls and is the X11 analogue of Wayland's compositor-owned move request.

Open questions:

- Whether LWJGL exposes enough Xlib/XCB helpers directly or whether a tiny JNI
  shim is cleaner.
- How to recover root coordinates and event timestamp from GLFW's mouse button
  callback path. GLFW reports cursor position in content coordinates; X11 may
  need a native query at the moment of the button press.

## Wayland Plan

Wayland is the platform that should drive the architecture.

Normal clients cannot set a toplevel's global position. Instead, xdg-shell
provides:

```text
xdg_toplevel.move(wl_seat, serial)
xdg_toplevel.resize(wl_seat, serial, edges)
xdg_toplevel.show_window_menu(wl_seat, serial, x, y)
```

The request must be sent in response to a user action such as pointer button
press or touch down. The `serial` is how the compositor verifies that the client
is reacting to the current input event. A stale, missing, or unrelated serial
may be ignored. If the move starts, device focus can be taken over by the
compositor.

### GLFW gap

GLFW native Wayland access currently exposes:

- `glfwGetWaylandDisplay()` -> `wl_display*`
- `glfwGetWaylandWindow(window)` -> the main `wl_surface*`

That is not enough by itself. To call `xdg_toplevel.move`, the host also needs:

- the `xdg_toplevel` for the GLFW window,
- the `wl_seat` associated with the triggering input device,
- the serial from the exact `wl_pointer.button` or `wl_touch.down` event.

GLFW has those internally for its Wayland backend, but they are not part of the
public native access API. This is the central implementation risk.

### Viable implementation paths

1. Add or upstream a GLFW API for interactive window operations.

   Ideal shape:

   ```c
   void glfwStartWindowMove(GLFWwindow* window);
   void glfwStartWindowResize(GLFWwindow* window, int edges);
   void glfwShowWindowMenu(GLFWwindow* window, int x, int y);
   ```

   GLFW can use its private Wayland `xdg_toplevel`, seat, and serial, while
   mapping to native APIs on other platforms. This is the cleanest long-term
   answer but depends on GLFW upstream or carrying a fork/patch.

2. Patch or fork GLFW locally for Wayland only.

   Add a private exported function or a small extension point in the bundled
   native library. This keeps correctness but increases native distribution
   burden.

3. Build a parallel Wayland input/window companion.

   Use `glfwGetWaylandDisplay()` and `glfwGetWaylandWindow()` plus registry
   bindings to track seats and input serials, then discover or own enough shell
   state to call `xdg_toplevel.move`. This is fragile because the shell role is
   owned by GLFW and `xdg_toplevel` is not exposed. It is likely not acceptable
   unless GLFW adds a way to share the toplevel.

4. Fall back to XWayland/manual positioning.

   This is not a real Wayland solution and should not be the plan.

### Wayland decoration idioms

Custom title bars are client-side decorations. On Wayland, that means the client
draws titlebar controls and uses xdg-shell requests to ask the compositor to
perform move, resize, and window menu operations.

`xdg-decoration` negotiates whether the compositor or client draws window
decorations. A compositor can choose client-side or server-side mode regardless
of the client's preference. If the app requests server-side decorations and gets
them, the custom titlebar should normally be disabled or treated as ordinary UI.
If it uses client-side decorations, it owns drawing and hit testing the
titlebar, resize borders, shadows, and buttons.

`libdecor` is a toolkit-style solution for client-side decorations when the
compositor does not provide server-side decorations. It can draw fallback
decorations and drive move/resize requests. It is useful background and may be
what GLFW uses internally, but it is not a good direct Compose GLFW API unless
we decide to hand decoration rendering to libdecor instead of Compose.

`xdg-toplevel-drag` is related but different. It moves a toplevel during a drag
and drop operation, primarily for detachable tabs, docks, and tool windows. It
is not needed for dragging an existing custom titlebar. It becomes relevant if
Compose GLFW later supports tearing content into new windows during DnD.

## Architecture

```text
WindowDraggableArea modifier
        |
        v
WindowDragRegionRegistry
  - records draggable bounds from Compose layout
  - hit-tests pointer/window coordinates
        |
        v
WindowDragController
  - startMove(trigger)
  - startResize(trigger, edges) later
  - showWindowMenu(trigger, position) later
        |
        +--> MacWindowDragController
        +--> Win32WindowDragController
        +--> X11WindowDragController
        +--> WaylandWindowDragController
        +--> NoOpWindowDragController
```

The registry matters most for Windows hit testing, where the OS can ask for a
nonclient region independently of Compose's pointer-input coroutine. It is also
useful for future resize borders and window menus.

Compose modifier behavior:

- Register its layout bounds with the window.
- On pointer down, ask `WindowDragController.startMove(...)`.
- Consume the event only if the native move is accepted.
- Avoid starting when the window is fullscreen.
- Avoid starting on secondary buttons unless implementing window-menu behavior.
- Let nested interactive controls opt out by consuming the pointer down first.

Backend behavior:

- Create the platform controller in `PlatformWindow`.
- Expose an internal `startInteractiveMove(...)`.
- Keep public API under `HostWindowScope`; do not expose native handles.
- Use `NoOpWindowDragController` where unsupported, returning `false`.

## TODO

- Add `roadmap/DRAGGABLE_WINDOW_AREA.md` to `ROADMAP.md`.
- Add `WindowDragController` and no-op implementation.
- Add `HostWindowScope.WindowDraggableArea`.
- Add a drag-region registry tied to `PlatformWindow`.
- macOS: fetch `NSWindow`/`NSView` via GLFW native access.
- macOS: install Cocoa event hook and call `performDrag(with:)`.
- macOS: demo an undecorated custom titlebar.
- Windows: subclass `HWND` and return `HTCAPTION` from `WM_NCHITTEST`.
- X11: send `_NET_WM_MOVERESIZE` move requests.
- Wayland: inspect GLFW Wayland backend and decide between upstream API, carried
  GLFW patch, or scoped native helper.
- Wayland: preserve event serial and seat for pointer/touch down if GLFW exposes
  or can be patched to expose them.
- Wayland: add resize/menu design alongside move because the same serial/seat
  plumbing is needed.
- Verify native snapping/tiling on Windows, X11, and Wayland compositors.
- Verify macOS Spaces, fullscreen, and multi-monitor behavior.

## Sources

- Compose Desktop `WindowDraggableArea` source from local Compose 1.11.1 cache:
  uses a manual AWT move handler by default and a JBR move handler where
  available.
- GLFW native access: https://www.glfw.org/docs/latest/group__native.html
- GLFW window position docs:
  https://www.glfw.org/docs/latest/window_guide.html#window_pos
- AppKit `NSWindow.performDrag(with:)`:
  https://developer.apple.com/documentation/appkit/nswindow/performdrag%28with%3A%29
- AppKit `isMovableByWindowBackground`:
  https://developer.apple.com/documentation/appkit/nswindow/ismovablebywindowbackground
- Win32 `WM_NCHITTEST`/`HTCAPTION`:
  https://learn.microsoft.com/en-us/windows/win32/inputdev/about-mouse-input
- EWMH `_NET_WM_MOVERESIZE`:
  https://specifications.freedesktop.org/wm/latest-single/#idm140200472615568
- xdg-shell `xdg_toplevel.move`, `resize`, and `show_window_menu`:
  https://wayland.app/protocols/xdg-shell
- Wayland Book, interactive move and resize:
  https://wayland-book.com/xdg-shell-in-depth/interactive.html
- Wayland `xdg-decoration`:
  https://wayland.app/protocols/xdg-decoration-unstable-v1
- Wayland `xdg-toplevel-drag`:
  https://wayland.app/protocols/xdg-toplevel-drag-v1
