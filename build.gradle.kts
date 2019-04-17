import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version Versions.kotlin
  application
  id("com.github.ben-manes.versions") version "0.21.0"
  id("com.jfrog.bintray") version "1.8.4"
  id("org.jetbrains.dokka") version "0.9.18"
  id("net.nemerosa.versioning") version "2.8.2"
  id("com.palantir.consistent-versions") version "1.5.0"
  id("com.diffplug.gradle.spotless") version "3.21.1"
  id("com.palantir.graal") version "0.3.0-6-g0b828af"
  id("com.hpe.kraal") version "0.0.15"
}

repositories {
  jcenter()
  mavenCentral()
  maven(url = "https://jitpack.io")
  maven(url = "http://repo.maven.apache.org/maven2")
  maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/")
  maven(url = "https://dl.bintray.com/yschimke/baulsupp.com/")
  maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

group = "com.baulsupp"
val artifactID = "cooee-cli"
description = "Coo.ee CLI"
val projectVersion = versioning.info.display!!
version = projectVersion

application {
  // Define the main class for the application
  mainClassName = "com.baulsupp.cooee.cli.Main"
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
    kotlinOptions.allWarningsAsErrors = false
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=enable")
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

graal {
  graalVersion("1.0.0-rc15")
  mainClass("com.baulsupp.cooee.cli.Main")
  outputName("cooee")
  option("--configurations-path")
  option("graal.config")
}

spotless {
  kotlinGradle {
    ktlint("0.31.0").userData(mutableMapOf("indent_size" to "2", "continuation_indent_size" to "2"))
    trimTrailingWhitespace()
    endWithNewline()
  }
}

dependencies {
  implementation("com.github.rvesse:airline")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("com.squareup.moshi:moshi")
  implementation("com.squareup.moshi:moshi-adapters")
  implementation("com.squareup.moshi:moshi-kotlin")
  implementation("com.squareup.okhttp3:okhttp")
  implementation("com.squareup.okio:okio")
  implementation("com.baulsupp:oksocial-output")
  implementation("com.baulsupp:okurl")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
  implementation("org.jline:jline")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

  testRuntime("org.junit.jupiter:junit-jupiter-engine")
  testRuntime("org.slf4j:slf4j-jdk14")
}
