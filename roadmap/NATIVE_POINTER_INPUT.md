# Native Pointer Input Plan

Compose GLFW currently receives mouse, scroll, keyboard, text, and IME input
through GLFW callbacks. GLFW does not expose a cross-platform touch or stylus
stream, so touch/stylus support should be layered beside the existing GLFW input
dispatcher and should feed Compose's multi-pointer scene API directly.

## Goals

- Preserve GLFW as the windowing and event-loop owner.
- Support real non-mouse pointer devices without degrading existing mouse,
  scroll, keyboard, text, or IME behavior.
- Normalize platform-specific contact streams into a small host model before
  creating Compose events.
- Start with macOS trackpad gestures, then add screen/stylus contacts on
  platforms where native APIs expose them.

## Non-Goals

- Do not switch the host to SDL as part of this feature.
- Do not expose every raw platform axis in the first pass.
- Do not synthesize fake multi-touch contacts from trackpad gestures unless
  Compose requires it for a specific gesture path.
- Do not route mouse input through the new adapter until non-mouse input is
  proven.

## Host Model

Add an internal native-pointer layer that owns non-mouse pointer state:

```kotlin
internal interface NativePointerInputAdapter : AutoCloseable {
  var enabled: Boolean
}

internal data class NativePointer(
  val id: Long,
  val position: Offset,
  val pressed: Boolean,
  val type: PointerType,
  val pressure: Float = 1f,
  val historical: List<HistoricalChange> = emptyList(),
)

internal data class NativePointerEvent(
  val type: PointerEventType,
  val pointers: List<NativePointer>,
  val scrollDelta: Offset = Offset.Zero,
  val scaleGestureFactor: Float = 1f,
  val panGestureOffset: Offset = Offset.Zero,
  val nativeEvent: Any? = null,
)
```

`InputDispatcher` should continue to own GLFW mouse, scroll, keyboard, text, and
IME callbacks. A separate adapter should be created by `WindowHost.attachPeer`
for the current platform. Both dispatchers send into `ComposeWindowScene`.

`ComposeWindowScene` should gain a second pointer entry point that accepts the
host model and calls Compose's `sendPointerEvent(eventType, pointers = ...)`
overload. The existing one-pointer mouse entry point should remain as-is for
GLFW.

## macOS Architecture

macOS input should attach to the Cocoa view/window exposed by GLFW. The adapter
should live under `internal/platform/macos` and be installed only when
`DisplayServer.COCOA` is active.

Preferred event sources:

- `NSResponder` gesture callbacks for trackpad gesture events:
  `beginGestureWithEvent:`, `magnifyWithEvent:`, `scrollWheel:`,
  `rotateWithEvent:`, `endGestureWithEvent:`.
- `NSEvent.touchesMatchingPhase:inView:` and `NSTouch.identity` when a gesture
  event exposes touch snapshots.
- Existing GLFW scroll callbacks for ordinary wheel/trackpad scroll until the
  native path can preserve better phase and momentum data.

Implementation shape:

- Create a small Objective-C subclass/proxy installed on the GLFW content view,
  or swizzle/forward through an associated responder object if subclassing the
  GLFW-created view is not practical.
- Preserve the original responder behavior. Native pointer handling must call
  through for events it does not consume.
- Convert AppKit view coordinates into the same framebuffer-pixel scene
  coordinates used by `InputDispatcher.updateMousePosition`.
- Treat trackpad pinch/rotate/pan as gesture events:
  - magnify phase begin/change/end -> `PointerEventType.ScaleStart`,
    `ScaleChange`, and `ScaleEnd` with `scaleGestureFactor`.
  - scroll/pan gesture phase -> `PanStart`, `PanMove`, `PanEnd` with
    `panGestureOffset` when the native event is gesture-like.
  - rotate should initially stay in the host model/native event only unless
    Compose exposes a matching scene field.
- For real touch snapshots, map `NSTouch.identity` to stable Compose pointer ids
  for the lifetime of the touch.
- Update `InputModeManager` from real touch/stylus events, not from mouse-only
  events once this path exists.

macOS caveats:

- Mac trackpads usually expose high-level gesture events, not screen-space
  finger contacts suitable for normal `detectTransformGestures`.
- AppKit touch snapshots are valid for gesture events and use touch identity
  objects rather than persistent event instances.
- macOS stylus support depends on tablet/sidecar/device paths and should be
  verified separately from trackpad gestures.

## Windows Follow-Up

Windows should use the GLFW Win32 window handle and pointer messages:

- Get the HWND with `glfwGetWin32Window`.
- Subclass the window procedure or register a message hook owned by the
  `PlatformWindow`.
- Handle `WM_POINTERDOWN`, `WM_POINTERUPDATE`, `WM_POINTERUP`,
  `WM_POINTERENTER`, `WM_POINTERLEAVE`, and `WM_POINTERCAPTURECHANGED`.
- Use pointer ids as stable Compose pointer ids.
- Use frame APIs such as `GetPointerFrameInfo` when multiple contacts are
  reported in one frame.
- Map touch to `PointerType.Touch`, pen to `PointerType.Stylus`, inverted pen to
  `PointerType.Eraser` when available.
- Map pressure/buttons into `NativePointer.pressure` and `PointerButtons`.

## Linux Follow-Up

Linux needs separate plans for Wayland and X11:

- Wayland: integrate with the seat used by the GLFW window if GLFW exposes
  enough handles, or defer until a safe Wayland-side attachment point exists.
  `wl_touch` batches contact changes with `wl_touch.frame`, so events should be
  accumulated and sent to Compose per frame.
- Wayland tablet/stylus: evaluate the tablet protocol separately from
  `wl_touch`.
- X11: evaluate XInput2 touch events and device coordinates; expect weaker
  compositor-level gesture support than macOS/Windows.
- If native backend ownership becomes too invasive, reassess SDL as the input
  abstraction for Linux rather than bolting a second event connection onto GLFW.

## TODO

- [ ] Add `NativePointerInputAdapter` and a no-op default implementation.
- [ ] Add `ComposeWindowScene.sendNativePointerEvent(...)` using Compose's
      multi-pointer scene API.
- [ ] Keep `InputDispatcher` mouse routing unchanged and mark it as the GLFW
      mouse/keyboard/text dispatcher.
- [ ] Extend `WindowPeer` to own and close both the GLFW input dispatcher and
      the native pointer adapter.
- [ ] Add macOS adapter scaffolding under `internal/platform/macos`.
- [ ] Add Objective-C runtime helpers for registering a small class with event
      methods or for installing a safe forwarding responder.
- [ ] Attach the macOS adapter to the GLFW Cocoa content view and restore the
      original state on close/recreate.
- [ ] Convert AppKit coordinates to framebuffer-pixel scene coordinates.
- [ ] Route macOS magnify gesture phases to Compose `ScaleStart`, `ScaleChange`,
      and `ScaleEnd` events.
- [ ] Route macOS pan/scroll gesture phases to Compose `PanStart`, `PanMove`,
      and `PanEnd` where phase data is available.
- [ ] Investigate whether macOS touch snapshots are available and useful for
      trackpad contacts in this host; if not, document trackpad support as
      gesture-based rather than contact-based.
- [ ] Add a demo panel that displays pointer type, ids, pressure, active count,
      pan offset, scale factor, and native-event class.
- [ ] Add manual verification notes for macOS trackpad pinch and pan.
- [ ] Add Windows `WM_POINTER` adapter behind a platform factory.
- [ ] Add Linux Wayland/X11 feasibility notes after testing native handles and
      event ownership constraints.
