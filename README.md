# Compose GLFW proof of concept

This is a small JVM Compose app hosted by LWJGL GLFW instead of Compose Desktop's AWT/Swing windowing layer.

The host creates a GLFW window, prefers GLFW's Wayland platform when `WAYLAND_DISPLAY` is set, creates an EGL/OpenGL context, gives Skia a GLFW-backed OpenGL proc loader through LWJGL, and renders a `CanvasLayersComposeScene` into the GLFW backbuffer.

## Run

```sh
mise run build
mise run run
```

For a non-interactive runtime check:

```sh
mise run smoke
```

The smoke task exits after two rendered frames and prints the selected GLFW platform plus a center-pixel readback. On this machine it verified:

```text
GLFW platform: Wayland
Smoke frame: 1632x1088, center rgba=(239,244,249,255)
```

`Failed to load plugin 'libdecor-gtk.so': failed to init` can appear on startup; GLFW still creates and runs the Wayland window.

## Notes

The Compose scene-hosting API used here is `InternalComposeUiApi`, because Compose Multiplatform does not currently expose a stable public desktop host API independent of AWT/Swing. The point of the POC is to verify that Compose rendering itself can be driven from a GLFW/Wayland loop.
