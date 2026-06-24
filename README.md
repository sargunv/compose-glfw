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

## Installation

> [!WARNING]
> This project uses internal Compose APIs and may break with future Compose
> versions; pay attention to the Compose version listed in the release notes.

Add the core library and the runtime modules you want to ship:

```kotlin
dependencies {
  implementation("dev.sargunv:compose-glfw:<version>")
  runtimeOnly("dev.sargunv:compose-glfw-opengl-linux-arm64:<version>")
  runtimeOnly("dev.sargunv:compose-glfw-opengl-linux-x64:<version>")
  runtimeOnly("dev.sargunv:compose-glfw-metal-macos-arm64:<version>")
  runtimeOnly("dev.sargunv:compose-glfw-metal-macos-x64:<version>")
  runtimeOnly("dev.sargunv:compose-glfw-direct3d-windows-x64:<version>")
  runtimeOnly("dev.sargunv:compose-glfw-direct3d-windows-arm64:<version>")
}
```

## Usage

Run your Compose content with `glfwApplication`:

```kotlin
fun main() = glfwApplication {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Example",
    state = rememberWindowState(size = DpSize(960.dp, 640.dp)),
  ) {
    App()
  }
}

@Composable
fun App() {
  // Your Compose UI.
}
```

You can pass a `Dp.Unspecified` dimension to window size to fit to contents.

### Window configuration

Configure the GLFW window further with `WindowOptions`:

```kotlin
Window(
  onCloseRequest = ::exitApplication,
  title = "Example",
  undecorated = true,
  transparent = true,
  resizable = true,
  focusOnShow = false,
  options = WindowOptions {
    textToolbar = { state, actions ->
      // Draw a custom text context menu / toolbar.
    }
  },
) {
  App()
}
```

### Custom cursors

Use `cursorImagePointerIcon` with the `pointerHoverIcon` modifier to use a
custom `ImageBitmap` cursor:

```kotlin
Modifier.pointerHoverIcon(
  cursorImagePointerIcon(image, imageScale = 1f, hotSpot = hotSpot),
)
```

### File drops

Use the `fileDropTarget` modifier to receive file drops delivered by the host:

```kotlin
Modifier.fileDropTarget { files ->
  files.paths.forEach { path ->
    // Handle dropped file.
  }
}
```

### File pickers

Use `LocalWindow` to show native file and folder pickers from composables:

```kotlin
@Composable
fun OpenImageButton() {
  val filePicker = LocalWindow.current.filePicker

  Button(
    onClick = {
      val image =
        filePicker.openFile(
          filters = listOf(FileDialogFilter("Images", listOf("png", "jpg", "jpeg"))),
        )
      // Handle selected path, or null if canceled.
    }
  ) {
    Text("Open image")
  }
}
```

On Linux, file pickers use `xdg-desktop-portal` so they work with your desktop
environment's file picker.

### Graphics interop

Advanced renderers can access the host graphics context from composables:

```kotlin
@Composable
fun RendererHost() {
  val renderContext = LocalWindow.current.renderContext

  val openGl = renderContext as? OpenGlRenderContext
  // or
  val metal = renderContext as? MetalRenderContext
  // or
  val direct3d = renderContext as? Direct3DRenderContext

  App()
}
```

### Platform notes

On macOS, apps must be launched on AppKit's first thread:

```sh
-XstartOnFirstThread
```

On recent JDKs, LWJGL may also require native access to be enabled:

```sh
--enable-native-access=ALL-UNNAMED
```

On Linux, by default, the host prefers Wayland when `WAYLAND_DISPLAY` is set.
You can force the GLFW display server backend with:

```sh
-Dcompose.glfw.platform=wayland
-Dcompose.glfw.platform=x11
```

### App icons

Compose GLFW does not expose GLFW's `glfwSetWindowIcon` API. Modern Linux
Wayland and macOS ignore that per-window API; app icons should come from
platform app metadata instead. Use Freedesktop desktop-entry/icon-theme metadata
on Linux, app bundle `Info.plist` icon metadata on macOS, and executable icon
resources on Windows.

## Status and limitations

The following features are supported:

- Application composition and window model
- Fractional scaling
- Resize, density, focus, and window info updates
- Pointer, scroll, and keyboard events
- Clipboard integration
- Popup/dropdown positioning
- Pointer cursor shapes and custom cursor images
- Light/dark theme detection
- Native file and folder pickers
- Graphics context access for advanced rendering integrations

The following features are partially supported:

- Native file pickers
  - On Wayland, portal dialogs are supported but are not parented to the GLFW
    window yet.
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
- Window state
  - Supports runtime size, minimize, maximize, fullscreen, and position updates.
  - Wayland restricts window positioning and always-on-top.

The following features are not yet supported:

- Screen reader, native menus, tray, or dialogs
- Interop views like `SwingPanel`. Advanced users can instead use the host
  graphics context for integrating custom components.
