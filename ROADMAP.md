# Roadmap

This file tracks implementation status for Compose GLFW.

Known gaps:

OS/platform API wiring:

- [ ] Windows light/dark mode detection
- [ ] Wayland file drops through GLFW
- [ ] Full native drag-and-drop parity beyond GLFW file drop callbacks:
      enter/move/action events, non-file payloads, and outgoing drags
- [ ] IME/preedit integration: composition text, candidate positioning, and
      commit/cancel lifecycle
- [ ] Touch/stylus input routing and `InputModeManager` updates on backends that
      can report non-mouse pointer devices
- [ ] Screen reader and accessibility integration
- [ ] Keep-screen-on and frame-rate voting
- [ ] Native macOS fullscreen Spaces (`NSWindow.toggleFullScreen`)
- [ ] Native menus
- [ ] System tray
- [ ] Dialogs and file pickers
- [ ] Draggable window area

Useful parity reference:
[Compose Multiplatform desktop-only API](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html).
