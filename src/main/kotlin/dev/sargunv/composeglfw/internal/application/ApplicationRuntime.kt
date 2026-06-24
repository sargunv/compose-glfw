package dev.sargunv.composeglfw.internal.application

import dev.sargunv.composeglfw.internal.platform.HostOperatingSystem
import dev.sargunv.composeglfw.internal.platform.hostOperatingSystem
import dev.sargunv.composeglfw.internal.platform.macos.MacObjectiveC
import org.lwjgl.system.Configuration

internal fun prepareApplicationRuntime() {
  if (hostOperatingSystem == HostOperatingSystem.LINUX) {
    prepareLinuxRuntime()
  }
  if (hostOperatingSystem == HostOperatingSystem.MACOS) {
    prepareMacOsRuntime()
  }
}

private fun prepareLinuxRuntime() {
  // Prefer xdg-desktop-portal for native file pickers so Wayland sessions do not fall back to GTK
  // dialogs running on the wrong display connection.
  Configuration.NFD_LINUX_PORTAL.set(true)
}

private fun prepareMacOsRuntime() {
  check(MacObjectiveC.isMainThread()) {
    "Compose GLFW on macOS must be launched on AppKit's first thread. Add -XstartOnFirstThread " +
      "to the JVM arguments."
  }

  val configuredHeadless = System.getProperty("java.awt.headless")
  require(configuredHeadless == null || configuredHeadless.equals("true", ignoreCase = true)) {
    "Compose GLFW on macOS requires AWT to remain headless because GLFW owns the AppKit " +
      "event loop. Remove -Djava.awt.headless=false or set -Djava.awt.headless=true."
  }

  // Compose GLFW owns the Cocoa event loop through GLFW. Compose UI currently starts Skiko's
  // Swing snapshot dispatcher internally, and if AWT initializes in non-headless mode it can
  // install its own NSApplication loop and make GLFW event polling block indefinitely.
  System.setProperty("java.awt.headless", "true")
}
