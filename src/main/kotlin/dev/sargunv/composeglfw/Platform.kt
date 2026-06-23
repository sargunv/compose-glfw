package dev.sargunv.composeglfw

import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND

public enum class DisplayServer(
  internal val supportsWindowPosition: Boolean,
  internal val reportsPreEventKeyModifiers: Boolean,
) {
  WAYLAND(
    supportsWindowPosition = false,
    reportsPreEventKeyModifiers = false,
  ),
  X11(
    supportsWindowPosition = true,
    reportsPreEventKeyModifiers = true,
  );

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

public enum class RenderBackend {
  OPENGL;

  public override fun toString(): String =
    when (this) {
      OPENGL -> "OpenGL"
    }
}
