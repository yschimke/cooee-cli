import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.publish.maven.MavenPom
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  kotlin("jvm") version Versions.kotlin
  application
  id("com.github.ben-manes.versions") version "0.20.0"
  id("org.jlleitschuh.gradle.ktlint") version "6.3.1"
  id("com.jfrog.bintray") version "1.8.4"
  id("org.jetbrains.dokka") version "0.9.17"
  id("net.nemerosa.versioning") version "2.8.2"
}

repositories {
  jcenter()
  mavenCentral()
  maven(url = "https://jitpack.io")
  maven(url = "http://repo.maven.apache.org/maven2")
  maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/")
  maven(url = "https://dl.bintray.com/yschimke/baulsupp.com/")
}

group = "com.baulsupp"
val artifactID = "cooee-cli"
description = "Coo.ee CLI"
val projectVersion = versioning.info.display!!
version = projectVersion

application {
  // Define the main class for the application
  mainClassName = "com.baulsupp.cooee.cli.CooeeCliKt"
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.3"
    kotlinOptions.languageVersion = "1.3"
  }
}

tasks.create("downloadDependencies") {
  description = "Downloads dependencies"

  doLast {
    configurations.forEach {
      if (it.isCanBeResolved) {
        it.resolve()
      }
    }
  }
}

dependencies {
  implementation(Deps.airline2)
  implementation(Deps.coroutinesCore)
  implementation(Deps.kotlinReflect)
  implementation(Deps.kotlinStandardLibrary)
  implementation(Deps.moshi)
  implementation(Deps.moshiAdapters)
  implementation(Deps.moshiKotlin)
  implementation(Deps.okhttp)
  implementation(Deps.okio)
  implementation(Deps.oksocialOutput)
  implementation(Deps.okurl)
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${Versions.kotlinCoroutines}")
  implementation("org.jline:jline:3.9.0")

  testImplementation(Deps.kotlinTest)
  testImplementation(Deps.kotlinTestJunit)

  testRuntime(Deps.junitJupiterEngine)
  testRuntime(Deps.slf4jJdk14)
}
