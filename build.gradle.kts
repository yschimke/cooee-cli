import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version Versions.kotlin
  application
  id("com.github.ben-manes.versions") version "0.21.0"
  id("com.jfrog.bintray") version "1.8.4"
  id("org.jetbrains.dokka") version "0.9.18"
  id("net.nemerosa.versioning") version "2.8.2"
  id("com.palantir.consistent-versions") version "1.9.2"
  id("com.diffplug.gradle.spotless") version "3.23.1"
  id("com.palantir.graal") version "0.3.0-37-g77aa98f"
  id("com.hpe.kraal") version "0.0.15"
  id("org.jetbrains.kotlin.kapt") version "1.3.41"
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

val os = "darwin"

graal {
  graalVersion("19.1.1")
  // https://github.com/palantir/gradle-graal/issues/105
  downloadBaseUrl("https://github.com/oracle/graal/releases/download/vm-19.1.1/graalvm-ce-darwin-amd64-19.1.1.tar.gz?a=")
  mainClass("com.baulsupp.cooee.cli.Main")
  outputName("cooee")
  option("--enable-http")
  option("--enable-https")
  option("-H:+ReportUnsupportedElementsAtRuntime")
  option("-H:+ReportExceptionStackTraces")
  option("-H:ReflectionConfigurationFiles=reflect.config")
  option("-H:+AddAllCharsets")
  option("--rerun-class-initialization-at-runtime=org.bouncycastle.crypto.prng.SP800SecureRandom")
  option("--rerun-class-initialization-at-runtime=org.bouncycastle.jcajce.provider.drbg.DRBG\$Default")
  option("--rerun-class-initialization-at-runtime=org.bouncycastle.jcajce.provider.drbg.DRBG\$NonceAndIV")
  option("--initialize-at-build-time=org.bouncycastle.util.Strings")
//  option("-J-Djava.security.properties=java.security.overrides")
  option("-J-Djava.net.preferIPv4Stack=true")
}

spotless {
  kotlinGradle {
    ktlint("0.31.0").userData(mutableMapOf("indent_size" to "2", "continuation_indent_size" to "2"))
    trimTrailingWhitespace()
    endWithNewline()
  }
}

dependencies {
  implementation(files("lib/defaults.jar"))
  implementation("info.picocli:picocli")
  implementation("info.picocli:picocli-codegen")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("com.squareup.moshi:moshi")
  implementation("com.squareup.moshi:moshi-adapters")
  implementation("com.squareup.moshi:moshi-kotlin")
  implementation("com.squareup.okhttp3:okhttp")
  implementation("com.squareup.okhttp3:logging-interceptor")
  implementation("com.squareup.okio:okio")
  implementation("com.baulsupp:oksocial-output")
  implementation("io.jsonwebtoken:jjwt-api")
  implementation("io.jsonwebtoken:jjwt-impl")
  implementation("io.jsonwebtoken:jjwt-jackson")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
  implementation("org.jline:jline")
  implementation("org.slf4j:slf4j-jdk14")

  kapt("com.squareup.moshi:moshi-kotlin-codegen")

  //   println(ReflectionConfigGenerator.generateReflectionConfig(CommandLine.Model.CommandSpec.forAnnotatedObject(Main())))
//  testImplementation("info.picocli:picocli-codegen:4.0.1")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

  testRuntime("org.junit.jupiter:junit-jupiter-engine")
  testRuntime("org.slf4j:slf4j-jdk14")
}
