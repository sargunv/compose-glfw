package dev.sargunv.composeglfw.internal.platform.windows

import org.lwjgl.system.windows.User32

internal fun configureDirectCompositionHost(hwnd: Long) {
  val style = User32.GetWindowLongPtr(hwnd, User32.GWL_EXSTYLE)
  val nextStyle = style or WS_EX_NOREDIRECTIONBITMAP.toLong()
  if (nextStyle != style) {
    User32.SetWindowLongPtr(null, hwnd, User32.GWL_EXSTYLE, nextStyle)
    User32.SetWindowPos(
      null,
      hwnd,
      User32.HWND_TOP,
      0,
      0,
      0,
      0,
      User32.SWP_NOMOVE or
        User32.SWP_NOSIZE or
        User32.SWP_NOZORDER or
        User32.SWP_NOACTIVATE or
        User32.SWP_FRAMECHANGED,
    )
  }
}

private const val WS_EX_NOREDIRECTIONBITMAP = 0x00200000
