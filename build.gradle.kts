import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  `java-library`
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.kotlinCompose)
}

repositories {
  google()
  mavenCentral()
}

val hostOs = OperatingSystem.current()!!
val hostArch = System.getProperty("os.arch")!!
val lwjglNativeClassifier =
  when {
    hostOs.isLinux && hostArch == "aarch64" -> "natives-linux-arm64"
    hostOs.isLinux -> "natives-linux"
    else -> null
  }
val skikoRuntime =
  when {
    hostOs.isLinux && hostArch == "aarch64" -> libs.skikoAwtRuntimeLinuxArm64
    hostOs.isLinux -> libs.skikoAwtRuntimeLinuxX64
    else -> null
  }

val demoRuntimeOnly = configurations.create("demoRuntimeOnly") {
  isCanBeConsumed = false
  isCanBeResolved = true
  extendsFrom(configurations.runtimeClasspath.get())
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaRelease.get())) } }

dependencies {
  api(libs.composeFoundation)
  api(libs.composeUi)
  api(platform(libs.lwjglBom))
  implementation(libs.lwjgl)
  implementation(libs.lwjglGlfw)
  implementation(libs.lwjglOpenGl)

  if (lwjglNativeClassifier != null && skikoRuntime != null) {
    demoRuntimeOnly(variantOf(libs.lwjgl) { classifier(lwjglNativeClassifier) })
    demoRuntimeOnly(variantOf(libs.lwjglGlfw) { classifier(lwjglNativeClassifier) })
    demoRuntimeOnly(variantOf(libs.lwjglOpenGl) { classifier(lwjglNativeClassifier) })
    demoRuntimeOnly(skikoRuntime)
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release = libs.versions.javaRelease.get().toInt()
}

tasks.register<JavaExec>("run") {
  group = "application"
  description = "Runs the local GLFW Compose demo."
  mainClass = "dev.sargunv.composeglfw.MainKt"
  classpath = sourceSets.main.get().output + demoRuntimeOnly
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
