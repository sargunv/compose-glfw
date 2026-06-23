# Compose GLFW

Compose GLFW is a JVM Compose host that runs Compose UI in a GLFW window instead of the default AWT/Swing desktop host.

I built this because the default Compose Desktop AWT/Swing host is a bad user experience, especially on Linux. Resize is slow, Wayland isn't supported, fractional scaling isn't supported, input functionality is limited, the GPU context isn't readily available for advanced rendering. Here I aim to fix all of that with a more robust windowing toolkit.

## Usage

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

Then run your Compose content with `glfwApplication`:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.*

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

## Window Options

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

## Custom Cursors

Use `cursorImagePointerIcon` when a Compose pointer hover modifier needs a backend-native cursor image:

```kotlin
Modifier.pointerHoverIcon(
  cursorImagePointerIcon(image, hotSpot),
)
```

## File Drops

Use `fileDropTarget` to receive file drops delivered by the host:

```kotlin
Modifier.fileDropTarget { files ->
  files.paths.forEach { path ->
    // Handle dropped file.
  }
}
```

## GPU Interop

Advanced renderers can access the host GPU context from the `Window` content scope:

```kotlin
Window(title = "Example") {
  val openGl = gpu as? OpenGlInterop

  App()
}
```

`OpenGlInterop` exposes the Skia `DirectContext`, EGL handles, GL proc address lookup, and a `makeCurrent` callback. This is intended for integrations that need to share the host OpenGL context, such as map or video renderers that produce textures consumed by Compose UI.

## Display Server Selection (Linux)

By default, the host prefers Wayland when `WAYLAND_DISPLAY` is set. You can force the GLFW display server backend with:
```sh
-Dcompose.glfw.platform=wayland
-Dcompose.glfw.platform=x11
```

## Supported

- Single window hosting for Compose UI on Linux with Wayland and X11
- Fractional scaling
- Resize, density, focus, and window info updates
- Pointer, scroll, and keyboard events
- Clipboard integration
- Popup/dropdown positioning
- Pointer cursor shapes and custom cursor images
- Light/dark theme detection
- Per-window GPU interop access for advanced OpenGL integrations

## Partially supported

- Compose drag-and-drop targets
  - Only file drops are supported, and only the final dropped file list event.
  - GLFW does not deliver enter, exit, drag, hover, move events.
  - GLFW does not currently deliver file drop callbacks to Wayland.
- Text input routing
  - Supports committed text from keyboard layouts, including normal text field editing.
  - Complex input methods are not fully supported yet, such as composing accented characters before committing them, choosing characters from CJK input method popups, or canceling an in-progress composition.

## Not yet supported

- Windows and macOS
- Dynamic multi-window composition
- Runtime window attribute mutation (`WindowState`)
- Screen reader, native menus, tray, dialogs, or file pickers
- Interop views like `SwingPanel`. Advanced users can instead use the host GPU context for integrating custom components.
