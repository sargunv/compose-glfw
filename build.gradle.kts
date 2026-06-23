import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinCompose)
}

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
        api(project.dependencies.platform(libs.lwjglBom))
        implementation(libs.lwjgl)
        implementation(libs.lwjglGlfw)
        implementation(libs.lwjglOpenGl)
      }
    }
  }
}
