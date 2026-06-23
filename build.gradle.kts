import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  application
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.kotlinCompose)
}

repositories {
  google()
  mavenCentral()
}

val hostOs = OperatingSystem.current()
val hostArch = System.getProperty("os.arch")
val lwjglNativeClassifier =
  when {
    hostOs.isLinux && hostArch == "aarch64" -> "natives-linux-arm64"
    hostOs.isLinux -> "natives-linux"
    else -> error("This proof of concept is currently wired for Linux hosts only")
  }

kotlin { compilerOptions { jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaRelease.get())) } }

application {
  mainClass = "dev.sargunv.composeglfw.MainKt"
  applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

dependencies {
  implementation(platform(libs.lwjglBom))
  implementation(libs.composeFoundation)
  implementation(libs.composeUi)
  implementation(libs.lwjgl)
  implementation(libs.lwjglGlfw)
  implementation(libs.lwjglOpenGl)
  runtimeOnly(variantOf(libs.lwjgl) { classifier(lwjglNativeClassifier) })
  runtimeOnly(variantOf(libs.lwjglGlfw) { classifier(lwjglNativeClassifier) })
  runtimeOnly(variantOf(libs.lwjglOpenGl) { classifier(lwjglNativeClassifier) })
  runtimeOnly(
    if (hostArch == "aarch64") libs.skikoAwtRuntimeLinuxArm64 else libs.skikoAwtRuntimeLinuxX64
  )
}

tasks.withType<JavaCompile>().configureEach {
  options.release = libs.versions.javaRelease.get().toInt()
}

tasks.withType<JavaExec>().configureEach {
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
