package dev.sargunv.composeglfw

import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND

public enum class GlfwPlatform(
  internal val supportsWindowPosition: Boolean,
) {
  WAYLAND(supportsWindowPosition = false);

  public override fun toString(): String =
    when (this) {
      WAYLAND -> "Wayland"
    }

  internal companion object {
    internal fun fromGlfwPlatform(platform: Int): GlfwPlatform =
      when (platform) {
        GLFW_PLATFORM_WAYLAND -> WAYLAND
        else -> error("Only GLFW Wayland is supported; GLFW selected platform id $platform")
      }
  }
}

public enum class GlfwRenderBackend {
  OPENGL;

  public override fun toString(): String =
    when (this) {
      OPENGL -> "OpenGL"
    }
}
