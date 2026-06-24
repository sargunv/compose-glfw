import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinCompose)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
}

group = "dev.sargunv"

version = providers.gradleProperty("composeGlfwVersion").getOrElse("0.0.0-SNAPSHOT")

val sourceLinkRef = if (version.toString().endsWith("SNAPSHOT")) "main" else "v$version"

repositories {
  google()
  mavenCentral()
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
        api(libs.composeFoundation)
        api(libs.composeUi)
        implementation(project.dependencies.platform(libs.dbusJavaBom))
        implementation(libs.dbusJavaCore)
        implementation(libs.dbusJavaTransportNativeUnixSocket)
        implementation(libs.lifecycleViewModel)
        implementation(libs.lifecycleViewModelSavedState)
        implementation(libs.navigationEvent)
        implementation(libs.savedState)
        api(project.dependencies.platform(libs.lwjglBom))
        implementation(libs.lwjgl)
        implementation(libs.lwjglGlfw)
        implementation(libs.lwjglNfd)
        implementation(libs.lwjglOpenGl)
      }
    }
  }
}

dokka {
  moduleName = "Compose GLFW"
  dokkaSourceSets.configureEach {
    includes.from("MODULE.md")
    sourceLink {
      remoteUrl("https://github.com/sargunv/compose-glfw/tree/$sourceLinkRef/")
      localDirectory.set(rootDir)
    }
  }
}

mavenPublishing {
  pom {
    name = "Compose GLFW"
    description = "JVM Compose host APIs for running Compose UI in GLFW windows."
    url = "https://github.com/sargunv/compose-glfw"
  }
}
