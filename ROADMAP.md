# Roadmap

This file tracks known gaps for Compose GLFW.

- Support Wayland parent-window integration for file picker dialogs
- Support Wayland file drag and drop
- Support rich drag and drop events (enter, move, action) and non-file payloads
- Support Touch/stylus input routing and `InputModeManager` updates for
  non-mouse pointer devices
- Support screen reader / accessibility integration
- Support keep screen on and frame rate voting
- Support native menus
- Support system tray
- Support draggable window area, especially on Wayland as it can't be done by
  just setting window position

Useful parity reference:
[Compose Multiplatform desktop-only API](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html).
