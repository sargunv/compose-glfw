package dev.sargunv.composeglfw

import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11

/** Display server used by GLFW for a window. */
public enum class DisplayServer(
  internal val supportsWindowPosition: Boolean,
  internal val reportsPreEventKeyModifiers: Boolean,
) {
  /** Wayland display server. */
  WAYLAND(
    supportsWindowPosition = false,
    reportsPreEventKeyModifiers = false,
  ),

  /** X11 display server. */
  X11(
    supportsWindowPosition = true,
    reportsPreEventKeyModifiers = true,
  );

  /** Returns the display server name shown in diagnostics and UI. */
  public override fun toString(): String =
    when (this) {
      WAYLAND -> "Wayland"
      X11 -> "X11"
    }

  internal companion object {
    internal fun fromGlfwPlatform(platform: Int): DisplayServer =
      when (platform) {
        GLFW_PLATFORM_WAYLAND -> WAYLAND
        GLFW_PLATFORM_X11 -> X11
        else -> error("Unsupported GLFW platform id $platform")
      }
  }
}

/** Renderer used to draw Compose content. */
public enum class RenderBackend {
  /** OpenGL renderer. */
  OPENGL;

  /** Returns the renderer name shown in diagnostics and UI. */
  public override fun toString(): String =
    when (this) {
      OPENGL -> "OpenGL"
    }
}
