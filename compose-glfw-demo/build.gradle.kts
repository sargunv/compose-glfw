import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinCompose)
}

repositories {
  google()
  mavenCentral()
}

val hostOs = System.getProperty("os.name")
val hostArch = System.getProperty("os.arch")
val isLinux = hostOs.equals("Linux", ignoreCase = true)
val hostRuntimeModule =
  when {
    isLinux && hostArch == "aarch64" -> ":compose-glfw-opengl-linux-arm64"
    isLinux -> ":compose-glfw-opengl-linux-x64"
    else -> null
  }

kotlin {
  explicitApi()

  jvm {
    compilerOptions { jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaRelease.get())) }
  }

  sourceSets {
    jvmMain {
      kotlin.srcDir("src/main/kotlin")
      dependencies {
        implementation(project(":"))
        implementation(libs.composeMaterial3)
        if (hostRuntimeModule != null) {
          runtimeOnly(project(hostRuntimeModule))
        }
      }
    }
  }
}

fun registerDemoRunTask(
  name: String,
  description: String,
  platform: String? = null,
) {
  tasks.register<JavaExec>(name) {
    group = "application"
    this.description = description
    mainClass = "dev.sargunv.composeglfw.demo.MainKt"
    classpath = files(tasks.named("jvmJar")) + configurations.named("jvmRuntimeClasspath").get()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    if (platform != null) {
      systemProperty("compose.glfw.platform", platform)
    }
  }
}

registerDemoRunTask(
  name = "run",
  description = "Runs the local GLFW Compose demo.",
)

registerDemoRunTask(
  name = "runWayland",
  description = "Runs the local GLFW Compose demo on Wayland.",
  platform = "wayland",
)

registerDemoRunTask(
  name = "runX11",
  description = "Runs the local GLFW Compose demo on X11.",
  platform = "x11",
)
