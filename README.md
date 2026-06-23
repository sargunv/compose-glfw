# Compose GLFW

[![Maven Central Version](https://img.shields.io/maven-central/v/dev.sargunv/compose-glfw?label=Maven)](https://central.sonatype.com/namespace/dev.sargunv)
[![Kotlin Version](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fsargunv%2Fcompose-glfw%2Frefs%2Fheads%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.kotlin&prefix=v&logo=kotlin&label=Kotlin)](./gradle/libs.versions.toml)
[![Compose Version](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fsargunv%2Fcompose-glfw%2Frefs%2Fheads%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.compose&prefix=v&logo=jetpackcompose&label=Compose)](./gradle/libs.versions.toml)
[![API Reference](https://img.shields.io/badge/API_Reference-blue?logo=Kotlin&logoColor=white)](https://sargunv.github.io/compose-glfw/)

This library provides a JVM Compose host that runs Compose UI in a GLFW window
instead of the default AWT/Swing desktop host.

I built this because the default Compose Desktop AWT/Swing host is a bad user
experience, especially on Linux. Resize is slow, Wayland isn't supported,
fractional scaling isn't supported, and the GPU context isn't readily available
for advanced rendering. Here I fix all of that with a more robust windowing
toolkit.

## Usage

> [!WARNING]
> This project uses internal Compose APIs and may break with future Compose
> versions; pay attention to the Compose version listed in the release notes.

Add the core library and the runtime modules you want to ship:

```kotlin
dependencies {
  implementation("dev.sargunv:compose-glfw:<version>")
  runtimeOnly("dev.sargunv:compose-glfw-opengl-linux-x64:<version>")
}
```

Available runtime modules:

- `compose-glfw-opengl-linux-x64`
- `compose-glfw-opengl-linux-arm64`
- macOS and Windows are TBD

Then run your Compose content with `glfwApplication`:

```kotlin
fun main() = glfwApplication {
  Window(
    title = "Example",
    size = DpSize(960.dp, 640.dp),
  ) {
    App()
  }
}

@Composable
fun App() {
  // Your Compose UI.
}
```

Configure the GLFW window with `WindowOptions`:

```kotlin
Window(
  title = "Example",
  options = WindowOptions {
    resizable = true
    transparentFramebuffer = true
    textToolbar = { state, actions ->
      // Draw a custom text context menu / toolbar.
    }
  },
) {
  App()
}
```

Use `cursorImagePointerIcon` with the `pointerHoverIcon` modifier to use a
custom `ImageBitmap` cursor:

```kotlin
Modifier.pointerHoverIcon(
  cursorImagePointerIcon(image, hotSpot),
)
```

Use the `fileDropTarget` modifier to receive file drops delivered by the host:

```kotlin
Modifier.fileDropTarget { files ->
  files.paths.forEach { path ->
    // Handle dropped file.
  }
}
```

Advanced renderers can access the host GPU context from the `Window` content
scope:

```kotlin
Window(title = "Example") {
  val openGl = gpu as? OpenGlInterop

  App()
}
```

On Linux, by default, the host prefers Wayland when `WAYLAND_DISPLAY` is set.
You can force the GLFW display server backend with:

```sh
-Dcompose.glfw.platform=wayland
-Dcompose.glfw.platform=x11
```

## Status

The following features are supported:

- Single window hosting for Compose UI on Linux with Wayland and X11
- Fractional scaling
- Resize, density, focus, and window info updates
- Pointer, scroll, and keyboard events
- Clipboard integration
- Popup/dropdown positioning
- Pointer cursor shapes and custom cursor images
- Light/dark theme detection
- Per-window GPU interop access for advanced OpenGL integrations

The following features are partially supported:

- Compose drag-and-drop targets
  - Only file drops are supported, and only the final dropped file list event.
  - GLFW does not deliver enter, exit, drag, hover, move events.
  - GLFW does not currently deliver file drop callbacks to Wayland.
- Text input routing
  - Supports committed text from keyboard layouts, including normal text field
    editing.
  - Complex input methods are not fully supported yet, such as composing
    accented characters before committing them, choosing characters from CJK
    input method popups, or canceling an in-progress composition.

The following features are not yet supported:

- Windows and macOS
- Dynamic multi-window composition
- Runtime window attribute mutation (`WindowState`)
- Screen reader, native menus, tray, dialogs, or file pickers
- Interop views like `SwingPanel`. Advanced users can instead use the host GPU
  context for integrating custom components.
