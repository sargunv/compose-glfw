package dev.sargunv.composeglfw.internal.platform

internal enum class HostOperatingSystem {
  LINUX,
  MACOS,
  OTHER,
}

internal val hostOperatingSystem: HostOperatingSystem =
  System.getProperty("os.name").orEmpty().let { name ->
    when {
      name.equals("Linux", ignoreCase = true) -> HostOperatingSystem.LINUX
      name.contains("Mac", ignoreCase = true) -> HostOperatingSystem.MACOS
      else -> HostOperatingSystem.OTHER
    }
  }
