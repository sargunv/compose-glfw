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
  runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-macos-arm64") })
  runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-macos-arm64") })
  runtimeOnly(variantOf(libs.lwjglNfd) { classifier("natives-macos-arm64") })
  runtimeOnly(libs.skikoAwtRuntimeMacosArm64)
}

mavenPublishing {
  pom {
    name = "Compose GLFW Metal macOS arm64 Runtime"
    description = "macOS arm64 Metal runtime dependencies for Compose GLFW."
    url = "https://github.com/sargunv/compose-glfw"
  }
}
