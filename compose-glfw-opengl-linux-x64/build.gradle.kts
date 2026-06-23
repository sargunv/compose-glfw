plugins {
  `java-library`
}

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
