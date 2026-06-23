# Roadmap

This file tracks implementation status for Compose GLFW.

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
