package dev.sargunv.composeglfw

import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11

/** Display server used by GLFW for a window. */
public enum class DisplayServer(
  internal val supportsWindowPosition: Boolean,
  internal val reportsPreEventKeyModifiers: Boolean,
  internal val glfwPlatformHint: Int,
) {
  /** Wayland display server. */
  WAYLAND(
    supportsWindowPosition = false,
    reportsPreEventKeyModifiers = false,
    glfwPlatformHint = GLFW_PLATFORM_WAYLAND,
  ),

  /** X11 display server. */
  X11(
    supportsWindowPosition = true,
    reportsPreEventKeyModifiers = true,
    glfwPlatformHint = GLFW_PLATFORM_X11,
  ),

  /** macOS Cocoa windowing backend. */
  COCOA(
    supportsWindowPosition = true,
    reportsPreEventKeyModifiers = false,
    glfwPlatformHint = GLFW_PLATFORM_COCOA,
  ),

  /** Win32 windowing platform. */
  WIN32(
    supportsWindowPosition = true,
    reportsPreEventKeyModifiers = true,
    glfwPlatformHint = GLFW_PLATFORM_WIN32,
  );

  /** Returns the display server name shown in diagnostics and UI. */
  public override fun toString(): String =
    when (this) {
      WAYLAND -> "Wayland"
      X11 -> "X11"
      COCOA -> "Cocoa"
      WIN32 -> "Win32"
    }

  internal fun displayConnectionName(): String? =
    when (this) {
      WAYLAND -> System.getenv("WAYLAND_DISPLAY")
      X11 -> System.getenv("DISPLAY")
      COCOA,
      WIN32 -> null
    }

  internal companion object {
    internal fun fromGlfwPlatform(platform: Int): DisplayServer =
      entries.firstOrNull { it.glfwPlatformHint == platform }
        ?: error("Unsupported GLFW platform id $platform")

    internal fun fromSelection(value: String): DisplayServer =
      when (value.lowercase()) {
        "wayland" -> WAYLAND
        "x11" -> X11
        "cocoa" -> COCOA
        "win32",
        "windows" -> WIN32
        else ->
          error("Unsupported GLFW platform '$value'. Use 'wayland', 'x11', 'cocoa', or 'win32'.")
      }
  }
}

/** Renderer used to draw Compose content. */
public enum class RenderBackend {
  /** OpenGL renderer. */
  OPENGL,

  /** Metal renderer. */
  METAL,

  /** Direct3D renderer. */
  DIRECT3D;

  /** Returns the renderer name shown in diagnostics and UI. */
  public override fun toString(): String =
    when (this) {
      OPENGL -> "OpenGL"
      METAL -> "Metal"
      DIRECT3D -> "Direct3D"
    }
}
