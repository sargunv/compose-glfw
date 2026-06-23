# Compose GLFW

Compose GLFW is a JVM Compose host that runs Compose UI in a GLFW window instead
of the default AWT/Swing desktop host.

## Project map

- `src/main/kotlin`: core Compose GLFW library.
- `compose-glfw-demo`: runnable demo application.
- `compose-glfw-opengl-linux-x64`: Linux x64 OpenGL runtime module.
- `compose-glfw-opengl-linux-arm64`: Linux arm64 OpenGL runtime module.
- `gradle`: Gradle wrapper and version catalog.

## Dev tool commands

- `mise run check`: run repository checks.
- `mise run fix`: apply automatic formatting fixes.
- `mise run build`: build all Gradle modules.
- `mise run run`: run the demo application.
- `mise run run-wayland`: run the demo with the Wayland GLFW backend.
- `mise run run-x11`: run the demo with the X11 GLFW backend.

Run `mise tasks ls --all` for the complete task list.

## Project invariants

<!-- List non-negotiable rules for this project -->
