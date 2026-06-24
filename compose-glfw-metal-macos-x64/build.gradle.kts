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
  runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-macos") })
  runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-macos") })
  runtimeOnly(libs.skikoAwtRuntimeMacosX64)
}

mavenPublishing {
  pom {
    name = "Compose GLFW Metal macOS x64 Runtime"
    description = "macOS x64 Metal runtime dependencies for Compose GLFW."
    url = "https://github.com/sargunv/compose-glfw"
  }
}
