package dev.sargunv.composeglfw.internal.platform

internal enum class HostOperatingSystem {
  LINUX,
  MACOS,
  WINDOWS,
  OTHER,
}

internal val hostOperatingSystem: HostOperatingSystem =
  System.getProperty("os.name").orEmpty().let { name ->
    when {
      name.equals("Linux", ignoreCase = true) -> HostOperatingSystem.LINUX
      name.contains("Mac", ignoreCase = true) -> HostOperatingSystem.MACOS
      name.startsWith("Windows", ignoreCase = true) -> HostOperatingSystem.WINDOWS
      else -> HostOperatingSystem.OTHER
    }
  }
