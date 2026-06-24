plugins {
  `java-library`
  alias(libs.plugins.mavenPublish)
}

group = "dev.sargunv"

version = providers.gradleProperty("composeGlfwVersion").getOrElse("0.0.0-SNAPSHOT")

repositories {
  google()
  mavenCentral()
}

dependencies {
  api(project(":"))
  runtimeOnly(platform(libs.lwjglBom))
  runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-windows-arm64") })
  runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-windows-arm64") })
  runtimeOnly(variantOf(libs.lwjglNfd) { classifier("natives-windows-arm64") })
  runtimeOnly(libs.skikoAwtRuntimeWindowsArm64)
}

mavenPublishing {
  pom {
    name = "Compose GLFW Direct3D Windows arm64 Runtime"
    description = "Windows arm64 Direct3D runtime dependencies for Compose GLFW."
    url = "https://github.com/sargunv/compose-glfw"
  }
}
