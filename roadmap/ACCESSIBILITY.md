# Accessibility Plan

Compose GLFW already receives Compose semantics owners through
`PlatformContext.SemanticsOwnerListener`, but it does not expose those semantics
to any operating-system accessibility API. The host keeps AWT headless on macOS
and lets GLFW own the native window/event loop, so Compose Desktop's AWT/Java
accessibility implementation is a reference for semantics mapping, not an
implementation path for this project.

## Goals

- Expose Compose semantics to native assistive technologies while preserving
  GLFW windowing and rendering.
- Keep the Compose-to-accessibility mapping host-neutral, with small
  platform-specific adapters.
- Start with macOS `NSAccessibility`, then add Windows UI Automation and Linux
  AT-SPI2 when the native object model is proven.
- Preserve stable native accessibility object identity across recomposition.

## Non-Goals

- Do not enable AWT as the accessibility transport.
- Do not add a vestigial Java Accessibility adapter unless a concrete test or
  compatibility need appears.
- Do not depend on GTK or Qt as Linux accessibility shims in the first design.
- Do not attempt full editable-text, table, or complex document parity in the
  first implementation pass.

## Platform APIs

There are four relevant accessibility APIs to understand:

1. Java Accessibility API / Java Access Bridge
   - Compose Desktop maps `SemanticsNode`s to `javax.accessibility.Accessible`
     and `AccessibleContext`.
   - On Windows, Java Access Bridge exposes that Java accessibility tree to
     screen readers.
   - On macOS, the JDK/AWT bridge maps Java accessibility objects onward to the
     native macOS accessibility system.
   - For Compose GLFW this is reference material only, because the host is not
     AWT-based.

2. macOS AppKit `NSAccessibility`
   - Native macOS accessibility API used by VoiceOver and Accessibility
     Inspector.
   - A GLFW Cocoa content view can be exposed as an accessibility container with
     virtual `NSAccessibilityElement` children backed by Compose semantics.
   - This is the first target.

3. Windows UI Automation
   - Native Windows accessibility and UI automation API used by screen readers
     and automation tools.
   - A GLFW HWND can expose a custom UIA provider with virtual fragment children
     backed by Compose semantics.
   - This is the Windows target, not Java Access Bridge.

4. Linux AT-SPI2
   - The de facto free-desktop accessibility IPC protocol, transported over
     D-Bus.
   - GTK, Qt, and browsers bridge their toolkit accessibility models to AT-SPI2,
     but a generic GLFW app must either implement/register an AT-SPI2 provider
     directly or deliberately add a toolkit/helper layer.
   - ATK is a toolkit-side abstraction, not the platform API for this host.

## Architecture

Add a host-neutral accessibility tree and platform adapters:

```text
Compose SemanticsOwner / SemanticsNode
        |
        v
HostAccessibilityTree
  - stable node cache by SemanticsNode.id
  - role/name/state/action/text mapping
  - bounds and hit-test helpers
  - semantic-change diffing
        |
        +--> MacAccessibilityAdapter(NSAccessibility)
        +--> WindowsAccessibilityAdapter(UI Automation)
        +--> LinuxAccessibilityAdapter(AT-SPI2)
```

`HostPlatformContext` should continue to collect semantics owners. A new
accessibility owner should subscribe to `SemanticsOwnerRegistry`, build/update
`HostAccessibilityNode` instances, and notify the active platform adapter when
nodes are added, removed, changed, focused, or moved.

The platform adapters should own only native object exposure and native event
publication. They should not duplicate Compose semantics traversal rules.

## Host Model

The host tree should cache enough information to serve synchronous native
accessibility queries without walking Compose internals on every call:

```kotlin
internal interface AccessibilityAdapter : AutoCloseable {
  fun onTreeChanged(change: AccessibilityTreeChange)
}

internal data class HostAccessibilityNode(
  val id: Int,
  val parentId: Int?,
  val childIds: List<Int>,
  val role: HostAccessibilityRole,
  val name: String?,
  val description: String?,
  val value: HostAccessibilityValue?,
  val state: HostAccessibilityState,
  val boundsInRoot: Rect,
  val boundsOnScreen: Rect,
  val actions: List<HostAccessibilityAction>,
)
```

The concrete shape can stay internal and evolve, but the important boundary is
that platform code receives normalized node data and callbacks for actions.

## Mapping

| Compose semantics                 | Host model                       | macOS `NSAccessibility`                        | Windows UIA                    | Linux AT-SPI2                |
| --------------------------------- | -------------------------------- | ---------------------------------------------- | ------------------------------ | ---------------------------- |
| `SemanticsNode.id`                | stable node id                   | stable `NSAccessibilityElement`                | provider/runtime id            | object path                  |
| parent/children                   | `parentId`, `childIds`           | `accessibilityParent`, `accessibilityChildren` | fragment navigation            | accessible parent/children   |
| `boundsInRoot`, screen position   | `boundsInRoot`, `boundsOnScreen` | `accessibilityFrame`                           | `BoundingRectangle`            | component extents            |
| `ContentDescription`, `Text`      | `name`, `description`            | label/title/help/value                         | Name/HelpText/Value/Text       | name/description/text        |
| `Role`                            | `HostAccessibilityRole`          | `NSAccessibility.Role`                         | `ControlType`                  | role                         |
| disabled/focused/selected/checked | `HostAccessibilityState`         | enabled/focused/selected/value attributes      | UIA properties                 | state set                    |
| click/expand/collapse/dismiss     | `HostAccessibilityAction`        | accessibility actions                          | Invoke/ExpandCollapse patterns | Action interface             |
| editable text/selection           | text value/ranges/actions        | text attributes/ranges                         | Text/TextEdit/Value patterns   | Text/EditableText interfaces |
| scroll/value/progress             | value/range actions              | value/actions                                  | RangeValue/Scroll patterns     | Value/actions                |
| semantic change                   | tree-change diff                 | accessibility notifications                    | UIA events/property changes    | AT-SPI events                |

## macOS First Pass

Implementation shape:

- Add `MacAccessibilityAdapter` under `internal/platform/macos`.
- Attach it to the GLFW Cocoa content view obtained from `PlatformWindow`.
- Make the content view an accessibility container, or install an associated
  accessibility object that returns virtual children.
- Represent each `HostAccessibilityNode` as a stable Objective-C accessibility
  element wrapper.
- Convert Compose framebuffer-pixel bounds to macOS screen coordinates.
- Use `NSWorkspace.isVoiceOverEnabled` for `PlatformScreenReader.isActive`.
- Post native notifications for focus, value, text, selected, and layout/tree
  changes.

MVP scope:

- Window/content root.
- Static text.
- Buttons and generic clickable nodes.
- Checkbox/radio/toggle state.
- Progress/value nodes.
- Focus state and hit testing.
- Accessibility Inspector verification before VoiceOver workflow testing.

Deferred macOS scope:

- Editable text, caret, and selection.
- Scroll containers and scroll actions.
- Tables/grids/lists.
- Custom actions beyond the common Compose semantics actions.
- Live regions and announcements.

## Windows Follow-Up

Implementation shape:

- Add a UI Automation provider for the GLFW HWND returned by
  `glfwGetWin32Window`.
- Expose the root as the provider for the window content and children as virtual
  fragment providers.
- Map host roles to UIA control types and host actions to UIA control patterns.
- Raise UIA property-change, structure-change, and focus events from host tree
  diffs.
- Use native screen-reader/activity detection only if a reliable API is
  available; otherwise treat `PlatformScreenReader.isActive` as best-effort.

Notes:

- Java Access Bridge should remain a reference, not the Windows implementation
  path, because this host is not Swing/AWT-based.
- UIA provider implementation likely needs additional COM helpers alongside the
  existing Direct3D COM utilities.

## Linux Follow-Up

Implementation shape:

- Target AT-SPI2 D-Bus interfaces directly or through a small helper library if
  one can expose the needed provider side without pulling in GTK/Qt ownership.
- Register the application/root object with the accessibility registry.
- Expose each host node as an AT-SPI object with role, state, component, action,
  text, value, and selection interfaces as needed.
- Emit AT-SPI object, state, property, text, focus, and children-changed events
  from host tree diffs.

Notes:

- This is independent of X11 vs Wayland at the accessibility-protocol layer,
  though coordinate and window identity details may still differ.
- GTK and Qt are useful references because they bridge to AT-SPI2, but their
  toolkit APIs are not the platform contract for Compose GLFW.

## TODO

- [ ] Add `HostAccessibilityTree` fed by `SemanticsOwnerRegistry`.
- [ ] Add normalized node role/name/state/action/value mapping.
- [ ] Add stable node cache keyed by `SemanticsNode.id`.
- [ ] Add tree diffing for add/remove/update/focus/layout changes.
- [ ] Add platform `AccessibilityAdapter` factory with a no-op default.
- [ ] Wire `HostPlatformContext.screenReader` to the platform adapter.
- [ ] Add macOS `NSAccessibility` adapter scaffolding.
- [ ] Expose the GLFW Cocoa content view as an accessibility container.
- [ ] Map static text, button, toggle, progress/value, and generic container
      nodes on macOS.
- [ ] Implement macOS hit testing and bounds conversion.
- [ ] Post macOS accessibility notifications for focus and content changes.
- [ ] Add an accessibility demo panel with semantic labels, roles, toggles,
      progress, and text.
- [ ] Verify macOS with Accessibility Inspector.
- [ ] Verify macOS with VoiceOver navigation and activation.
- [ ] Research and prototype a Windows UIA provider for the GLFW HWND.
- [ ] Research provider-side AT-SPI2 registration without GTK/Qt.

## References

- Compose Desktop accessibility:
  https://kotlinlang.org/docs/multiplatform/compose-desktop-accessibility.html
- Compose semantics:
  https://kotlinlang.org/docs/multiplatform/compose-accessibility.html
- Java Accessibility API:
  https://docs.oracle.com/javase/8/docs/api/javax/accessibility/AccessibleContext.html
- Java Access Bridge:
  https://docs.oracle.com/en/java/javase/25/access/java-access-bridge-overview.html
- macOS `NSAccessibilityElement`:
  https://developer.apple.com/documentation/appkit/nsaccessibilityelement
- Windows UI Automation:
  https://learn.microsoft.com/en-us/windows/win32/winauto/entry-uiauto-win32
- AT-SPI2: https://www.freedesktop.org/wiki/Accessibility/AT-SPI2/
