# Compose GLFW

Compose GLFW is a JVM Compose host that runs Compose UI in a GLFW window instead of the default AWT/Swing desktop host.

The current implementation is still early, but the intended library shape is in place: an app brings its own `@Composable fun App()` and runs it inside our host.

```kotlin
fun main() = glfwApplication {
  Window(
    title = "Example",
    options = WindowOptions {
      transparentFramebuffer = true
    },
  ) {
    App()
  }
}
```

## Current Status

What works today:

- [x] Linux runtime modules for OpenGL:
  - `compose-glfw-opengl-linux-x64`
  - `compose-glfw-opengl-linux-arm64`
- [x] GLFW window creation
- [x] Wayland-preferred GLFW platform selection
- [x] X11 GLFW platform selection
- [x] EGL/OpenGL context creation
- [x] Skia `DirectContext` creation through the GLFW GL proc loader
- [x] `CanvasLayersComposeScene` rendering into the GLFW backbuffer
- [x] Host-owned Compose scene coroutine dispatch on the GLFW UI thread
- [x] Resize handling
- [x] Content scale/density handling
- [x] Transparent framebuffer window option
- [x] Compose `WindowInfo` focus, size, and keyboard modifier state
- [x] Compose local/window coordinate conversion for framebuffer-scaled windows
- [x] Basic popup/dropdown positioning through `PlatformContext`
- [x] Linux system light/dark theme detection through XDG Desktop Portal settings
- [x] Window focus requests from Compose
- [x] Mouse move, press, and release
- [x] Scroll wheel and trackpad events
- [x] Basic keyboard key down, key up, and repeat routing
- [x] Keyboard modifier and lock-state propagation for pointer events
- [x] GLFW key table audited against Compose desktop key codes
- [x] Built-in pointer cursor shape updates
- [x] Custom cursor images
- [x] GLFW file drop callbacks into Compose drag-and-drop targets on X11
- [x] Basic committed text input from keyboard layouts
- [x] System text clipboard through Compose Desktop's clipboard implementation
- [x] Basic Compose-rendered text selection toolbar
- [x] Per-window text selection toolbar customization
- [x] Public `glfwApplication { Window { ... } }` API
- [x] Per-window GPU interop hook for the current OpenGL context
- [x] Platform lifecycle, ViewModel, saved-state, and navigation owners
- [x] Test root and semantics owner listener tracking

Known gaps:

Application composition and window model:

- [ ] Compose-style `glfwApplication { Window(...) }` application composition, instead of the current static startup window list
- [ ] Dynamic multi-window lifecycle: windows created and disposed as application composition changes
- [ ] `WindowState` parity with Compose Desktop `WindowState`: position, size, minimized, maximized, fullscreen
- [ ] Runtime window attribute updates from composition state: title, resizable, enabled/focusable where GLFW supports them
- [ ] Close-request flow matching Compose Desktop: `onCloseRequest` lets the app decide whether to close one window or exit
- [ ] Production-ready application lifecycle semantics

OS/platform API wiring:

- [ ] Wayland file drops through GLFW
- [ ] Full native drag-and-drop parity beyond GLFW file drop callbacks: enter/move/action events, non-file payloads, and outgoing drags
- [ ] IME/preedit integration: composition text, candidate positioning, and commit/cancel lifecycle
- [ ] Screen reader and accessibility integration
- [ ] Keep-screen-on and frame-rate voting
- [ ] Native menus, tray, dialogs, and file pickers

Packaging and platform expansion:

- [ ] Packaging/publishing metadata and documented consumer setup
- [ ] macOS backend/runtime modules
- [ ] Windows backend/runtime modules

Useful parity reference: [Compose Multiplatform desktop-only API](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html).

## Run

This runs the current visual demo app for manual verification:

```sh
mise run build
mise run run
mise run run-wayland
mise run run-x11
```

`mise run run` uses the host default, preferring Wayland when `WAYLAND_DISPLAY` is set. Use
`mise run run-wayland` or `mise run run-x11` to force a platform. Applications can use the same
selector with `-Dcompose.glfw.platform=wayland|x11`.

`Failed to load plugin 'libdecor-gtk.so': failed to init` can appear on Wayland startup; GLFW still creates and runs the window.

## Module Shape

The root artifact is the backend-neutral API/core artifact:

```text
compose-glfw
```

Backend/platform runtime artifacts carry native dependencies:

```text
compose-glfw-opengl-linux-x64
compose-glfw-opengl-linux-arm64
```

Future artifacts should include the rendering backend in the coordinate:

```text
compose-glfw-metal-macos-arm64
compose-glfw-metal-macos-x64
compose-glfw-direct3d-windows-x64
```

Do not publish LWJGL natives from the core artifact. Runtime artifacts should depend on core and add only the platform/backend native dependencies.

## Architecture

The production library should keep the public API narrow and put complexity behind internal boundaries:

```text
public API -> application/window model
platform   -> native windows, clipboard, IME, dialogs, menus
render     -> Skia surface/backend lifecycle
scene/input -> ComposeScene, invalidation, event translation
interop    -> minimal GPU context/texture hooks
```

Suggested internal contracts:

```kotlin
internal interface PlatformWindow

internal interface RenderBackendDriver {
  fun configureWindowHints()
  fun attach(window: PlatformWindow): RenderTarget
  fun resize(target: RenderTarget, width: Int, height: Int): RenderTarget
  fun render(target: RenderTarget, scene: ComposeWindowScene, frameTimeNanos: Long)
  fun swapBuffers(window: PlatformWindow)
}
```

Current backend:

```text
OpenGlRenderBackend
```

Future backends should be added behind this boundary. Do not standardize on Vulkan unless Skiko exposes a clean JVM Vulkan surface. In the current Skiko API, GL, Metal, and Direct3D are exposed; Vulkan is not.

## GPU Interop

Some apps need to render native GPU content, such as maps or video, into a texture and then sample that texture inside Compose UI. Compose Desktop does not expose this cleanly today, so apps rely on reflection into Skiko internals.

This host should expose the minimum resources necessary and no larger framework:

```kotlin
interface HostWindowScope {
  val gpu: GpuInterop
}

sealed interface GpuInterop {
  val backend: RenderBackend
}

data class OpenGlInterop(
  val directContext: DirectContext,
  val eglDisplay: Long,
  val eglConfig: Long,
  val eglContext: Long,
  val getProcAddress: Long,
  val resolveProcAddress: (String) -> Long,
  val makeCurrent: () -> Unit,
) : GpuInterop {
  override val backend = RenderBackend.OPENGL
}

enum class RenderBackend {
  OPENGL,
  METAL,
  DIRECT3D,
}
```

For the current Linux/OpenGL backend, the important values are:

- Skia `DirectContext`
- EGL display
- EGL config
- EGL context to share with
- GL proc address function
- `makeCurrent` callback for the host context

That is enough for a renderer such as MapLibre Native to create or borrow a shared GL texture and render into it. Presentation can live outside this library: downstream code can use public Compose/Skia APIs such as `drawIntoCanvas`, `skiaCanvas`, `BackendRenderTarget.makeGL`, `Surface.makeFromBackendRenderTarget`, and `Image` snapshots to draw the texture in a `Canvas`.

This keeps the host API to one expert-level hook: access to the owning window's GPU context. Texture lifecycle, synchronization, snapshot retention, and renderer-specific target descriptors belong in the integration layer that owns the native renderer.

## Notes

The Compose scene-hosting API used here is `InternalComposeUiApi`, because Compose Multiplatform does not currently expose a stable public desktop host API independent of AWT/Swing.
