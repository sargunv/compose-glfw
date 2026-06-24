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
  runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-windows") })
  runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-windows") })
  runtimeOnly(libs.skikoAwtRuntimeWindowsX64)
}

mavenPublishing {
  pom {
    name = "Compose GLFW Direct3D Windows x64 Runtime"
    description = "Windows x64 Direct3D runtime dependencies for Compose GLFW."
    url = "https://github.com/sargunv/compose-glfw"
  }
}
