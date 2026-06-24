# Roadmap

This file tracks implementation status for Compose GLFW.

Known gaps:

Application composition and window model:

- [ ] Event loop waiting: use `glfwWaitEvents`/wakeups instead of polling every
      loop when there is no pending Compose or GLFW work
- [ ] Window lifecycle state for hidden and minimized windows; focus currently
      maps to STARTED/RESUMED but visibility does not lower lifecycle state
- [ ] Propagate recomposer/coroutine failures out of the application loop before
      shutdown

OS/platform API wiring:

- [ ] Wayland file drops through GLFW
- [ ] Full native drag-and-drop parity beyond GLFW file drop callbacks:
      enter/move/action events, non-file payloads, and outgoing drags
- [ ] IME/preedit integration: composition text, candidate positioning, and
      commit/cancel lifecycle
- [ ] Touch/stylus input routing and `InputModeManager` updates on backends that
      can report non-mouse pointer devices
- [ ] Screen reader and accessibility integration
- [ ] Keep-screen-on and frame-rate voting
- [ ] Native menus, tray, dialogs, and file pickers

Packaging and platform expansion:

- [ ] macOS backend/runtime modules
- [ ] Windows backend/runtime modules

Useful parity reference:
[Compose Multiplatform desktop-only API](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html).
