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
  runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-linux") })
  runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-linux") })
  runtimeOnly(variantOf(libs.lwjglOpenGl) { classifier("natives-linux") })
  runtimeOnly(libs.skikoAwtRuntimeLinuxX64)
}

mavenPublishing {
  pom {
    name = "Compose GLFW OpenGL Linux x64 Runtime"
    description = "Linux x64 OpenGL runtime dependencies for Compose GLFW."
    url = "https://github.com/sargunv/compose-glfw"
  }
}
