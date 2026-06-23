# Roadmap

This file tracks implementation status for Compose GLFW.

Known gaps:

Application composition and window model:

- [ ] Compose Desktop-style `DpSize.Unspecified` content-driven window sizing
- [ ] Runtime window icon support
- [ ] Runtime transparent-window changes via native window recreation
- [ ] Production-ready application lifecycle semantics

OS/platform API wiring:

- [ ] Wayland file drops through GLFW
- [ ] Full native drag-and-drop parity beyond GLFW file drop callbacks:
      enter/move/action events, non-file payloads, and outgoing drags
- [ ] IME/preedit integration: composition text, candidate positioning, and
      commit/cancel lifecycle
- [ ] Screen reader and accessibility integration
- [ ] Keep-screen-on and frame-rate voting
- [ ] Native menus, tray, dialogs, and file pickers

Packaging and platform expansion:

- [ ] Packaging/publishing metadata and documented consumer setup
- [ ] macOS backend/runtime modules
- [ ] Windows backend/runtime modules

Useful parity reference:
[Compose Multiplatform desktop-only API](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html).
