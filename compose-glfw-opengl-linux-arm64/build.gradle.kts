plugins {
  `java-library`
}

repositories {
  google()
  mavenCentral()
}

dependencies {
  api(project(":"))
  runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-linux-arm64") })
  runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-linux-arm64") })
  runtimeOnly(variantOf(libs.lwjglOpenGl) { classifier("natives-linux-arm64") })
  runtimeOnly(libs.skikoAwtRuntimeLinuxArm64)
}
