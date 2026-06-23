# Compose GLFW

Compose GLFW is a JVM Compose host that runs Compose UI in a GLFW window instead of the default AWT/Swing desktop host.

The current implementation is still early, but the intended library shape is in place: an app brings its own `@Composable fun App()` and runs it inside our host.

```kotlin
fun main() = glfwApplication {
  Window(title = "Example") {
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
- [x] EGL/OpenGL context creation
- [x] Skia `DirectContext` creation through the GLFW GL proc loader
- [x] `CanvasLayersComposeScene` rendering into the GLFW backbuffer
- [x] Host-owned Compose scene coroutine dispatch on the GLFW UI thread
- [x] Resize handling
- [x] Content scale/density handling
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
- [x] Basic committed text input from keyboard layouts
- [x] System text clipboard through Compose Desktop's clipboard implementation
- [x] Basic Compose-rendered text selection toolbar
- [x] Per-window text selection toolbar customization
- [x] Public `glfwApplication { Window { ... } }` API
- [x] Per-window GPU interop hook for the current OpenGL context

Known gaps:

- [ ] Production-ready application lifecycle semantics
- [ ] Multi-window lifecycle beyond the current static startup window list
- [ ] X11 platform support
- [ ] Full key-event modifier payload for AltGraph and lock states
- [ ] IME/preedit integration: composition text, candidate positioning, and commit/cancel lifecycle
- [ ] Custom cursor images
- [ ] Drag and drop, including GLFW file drop callbacks
- [ ] Transparent framebuffer/window option
- [ ] Platform lifecycle/ViewModel owners
- [ ] Test root and semantics owner listeners
- [ ] Screen reader and accessibility integration
- [ ] Keep-screen-on and frame-rate voting
- [ ] Window state APIs: position, minimize, maximize, fullscreen, close requests
- [ ] Window decorations and styling controls
- [ ] Tooltips and layered windows
- [ ] Native menus, tray, dialogs, and file pickers
- [ ] Packaging/publishing metadata and documented consumer setup
- [ ] macOS backend/runtime modules
- [ ] Windows backend/runtime modules

Useful parity reference: [Compose Multiplatform desktop-only API](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html).

## Run

This runs the current visual demo app for manual verification:

```sh
mise run build
mise run run
```

`Failed to load plugin 'libdecor-gtk.so': failed to init` can appear on startup; GLFW still creates and runs the Wayland window.

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

internal interface RenderBackend {
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
interface GlfwWindowScope {
  val gpu: GlfwGpuInterop
}

sealed interface GlfwGpuInterop {
  val backend: GlfwRenderBackend
}

data class GlfwOpenGlInterop(
  val directContext: DirectContext,
  val eglDisplay: Long,
  val eglConfig: Long,
  val eglContext: Long,
  val getProcAddress: Long,
  val makeCurrent: () -> Unit,
) : GlfwGpuInterop {
  override val backend = GlfwRenderBackend.OPENGL
}

enum class GlfwRenderBackend {
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
