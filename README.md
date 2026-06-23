# Compose GLFW

Compose GLFW is a JVM Compose host that runs Compose UI in a GLFW window instead of the default AWT/Swing desktop host.

The current implementation is still a proof-bearing prototype, but the project shape should move toward a small library: an app brings its own `@Composable fun App()` and runs it inside our host.

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
- [x] Resize handling
- [x] Content scale/density handling
- [x] Mouse move, press, and release
- [x] Smoke run with center-pixel readback

Known gaps:

- [ ] Public `glfwApplication { Window { ... } }` API
- [ ] Move demo UI out of the library artifact
- [ ] Multi-window lifecycle
- [ ] Keyboard events and key mapping
- [ ] Text input and IME
- [ ] Scroll, hover, cursor icons
- [ ] Clipboard
- [ ] Window state: position, minimize, maximize, fullscreen, close requests
- [ ] Popups, tooltips, and layered windows
- [ ] Drag and drop
- [ ] Menus, tray, dialogs, and file pickers
- [ ] Accessibility
- [ ] macOS backend/runtime modules
- [ ] Windows backend/runtime modules

## Run

```sh
mise run build
mise run run
```

For a non-interactive runtime check:

```sh
COMPOSE_GLFW_EXIT_AFTER_FRAMES=2 ./gradlew run
```

On this machine the smoke run currently verifies Wayland rendering:

```text
GLFW platform: Wayland
Smoke frame: 1632x1088, center rgba=(239,244,249,255)
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

That is enough for a renderer such as MapLibre Native to create or borrow a shared GL texture, render into it, and then let the Compose host wrap that texture as a Skia backend render target for drawing in a `Canvas`.

The draw helper can stay tiny and backend-specific:

```kotlin
fun DrawScope.drawGlTexture(
  textureName: Int,
  textureTarget: Int,
  width: Int,
  height: Int,
  origin: GlfwTextureOrigin = GlfwTextureOrigin.BOTTOM_LEFT,
)
```

Internally this helper can create a framebuffer for the texture, wrap it with `BackendRenderTarget.makeGL`, create a Skia `Surface`, snapshot it as an `Image`, and draw that image into the Compose canvas. It should retain snapshots long enough for recorded Compose frames, matching the pattern used by the MapLibre compose-map prototype.

Keep this API explicitly expert-level. It exposes native handles, has thread/context affinity, and is only valid for the owning window/backend.

## Notes

The Compose scene-hosting API used here is `InternalComposeUiApi`, because Compose Multiplatform does not currently expose a stable public desktop host API independent of AWT/Swing.
