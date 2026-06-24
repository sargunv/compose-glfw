package dev.sargunv.composeglfw.internal.render

import dev.sargunv.composeglfw.DisplayServer
import dev.sargunv.composeglfw.internal.platform.currentDisplayServer
import dev.sargunv.composeglfw.internal.render.metal.MetalRenderBackend
import dev.sargunv.composeglfw.internal.render.opengl.OpenGlRenderBackend
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import dev.sargunv.composeglfw.internal.window.WindowClientApi

internal fun currentWindowClientApi(): WindowClientApi =
  when (currentDisplayServer()) {
    DisplayServer.WAYLAND,
    DisplayServer.X11 -> WindowClientApi.OPENGL_EGL
    DisplayServer.COCOA -> WindowClientApi.NO_API
  }

internal fun createRenderBackend(window: PlatformWindow): RenderBackendDriver =
  when (currentDisplayServer()) {
    DisplayServer.WAYLAND,
    DisplayServer.X11 -> OpenGlRenderBackend(window)
    DisplayServer.COCOA -> MetalRenderBackend(window)
  }
