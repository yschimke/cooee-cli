import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
  kotlin("jvm") version "1.4.31"
  kotlin("kapt") version "1.4.31"
  `maven-publish`
  application
  id("net.nemerosa.versioning") version "2.13.1"
  id("com.diffplug.spotless") version "5.1.0"
  id("com.palantir.graal") version "0.7.1"
  id("com.squareup.wire") version "3.4.0"
}

repositories {
  jcenter()
  mavenCentral()
  maven(url = "https://jitpack.io")
  maven(url = "https://oss.jfrog.org/oss-snapshot-local")
}

group = "com.baulsupp"
val artifactID = "cooee-cli"
description = "Coo.ee CLI"
val projectVersion = versioning.info.display!!
version = projectVersion

application {
  // Define the main class for the application
  mainClass.set("com.baulsupp.cooee.cli.Main")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.allWarningsAsErrors = false
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=enable", "-Xopt-in=kotlin.RequiresOptIn")
  }
}

wire {
  kotlin {
    out = "src/main/kotlin"
    javaInterop = true
  }
}

graal {
  mainClass("com.baulsupp.cooee.cli.Main")
  outputName("cooee")
  graalVersion("21.0.0.2")
  javaVersion("11")

  option("--enable-https")
  option("--no-fallback")
  option("--allow-incomplete-classpath")

  if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    // May be possible without, but autodetection is problematic on Windows 10
    // see https://github.com/palantir/gradle-graal
    // see https://www.graalvm.org/docs/reference-manual/native-image/#prerequisites
    windowsVsVarsPath("C:\\Program Files (x86)\\Microsoft Visual Studio\\2019\\BuildTools\\VC\\Auxiliary\\Build\\vcvars64.bat")
  }
}

dependencies {
  implementation("info.picocli:picocli:4.5.2")
  implementation("com.squareup.moshi:moshi:1.11.0")
  implementation("com.squareup.moshi:moshi-adapters:1.11.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
  implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")
  implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
  implementation("com.squareup.okhttp3:okhttp-brotli:5.0.0-alpha.2")
  implementation("com.github.yschimke:oksocial-output:6.1") {
    exclude(group= "org.jetbrains.kotlinx")
  }
  implementation("io.jsonwebtoken:jjwt-api:0.11.2")
  implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
  implementation("io.jsonwebtoken:jjwt-jackson:0.11.2")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.31")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.31")
  implementation("org.jline:jline-console:3.16.0")
  implementation("org.jline:jline-terminal:3.16.0")
  implementation("org.jline:jline-terminal-jansi:3.16.0")
  implementation("org.jline:jline-reader:3.16.0")
  implementation("org.jline:jline-style:3.16.0")
  implementation("org.slf4j:slf4j-jdk14:2.0.0-alpha1")
  implementation("com.github.yschimke:okurl:3.1") {
    isTransitive = false
  }

  kapt("com.squareup.moshi:moshi-kotlin-codegen:1.11.0")
  kapt("info.picocli:picocli-codegen:4.5.2")
  implementation("org.graalvm.nativeimage:svm:21.0.0.2") {
    // https://youtrack.jetbrains.com/issue/KT-29513
    exclude(group= "org.graalvm.nativeimage")
    exclude(group= "org.graalvm.truffle")
//    exclude(group= "org.graalvm.sdk")
    exclude(group= "org.graalvm.compiler")
  }
  implementation("io.github.classgraph:classgraph:4.8.87")

  implementation("io.swagger.parser.v3:swagger-parser:2.0.21")

  implementation("io.rsocket.kotlin:rsocket-core-jvm:0.13.0-SNAPSHOT")
  implementation("io.rsocket.kotlin:rsocket-transport-ktor-client:0.13.0-SNAPSHOT")
  implementation("io.ktor:ktor-network-tls:1.5.2")
  implementation("io.ktor:ktor-client-okhttp:1.5.2") {
    exclude(group= "com.squareup.okhttp3")
  }
  implementation("io.ktor:ktor-client-core-jvm:1.5.2") {
    exclude(group= "com.squareup.okhttp3")
  }

  implementation("com.squareup.wire:wire-runtime:3.4.0")
  implementation("com.squareup.wire:wire-moshi-adapter:3.4.0")
  implementation("com.squareup.wire:wire-grpc-client:3.4.0") {
    exclude(group= "com.squareup.okhttp3")
  }
  implementation("com.squareup.wire:wire-moshi-adapter:3.4.0")

  testImplementation("org.jetbrains.kotlin:kotlin-test:1.4.31")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.4.31")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

val sourcesJar by tasks.registering(Jar::class) {
  classifier = "sources"
  from(sourceSets.main.get().allSource)
}

publishing {
  publications {
    register("mavenJava", MavenPublication::class) {
      from(components["java"])
      artifact(sourcesJar.get())
    }
  }
}

if (properties.containsKey("graalbuild")) {
  val nativeImage = tasks["nativeImage"]

  distributions {
    val graal = create("graal") {
      contents {
        from("${rootProject.projectDir}") {
          include("README.md", "LICENSE")
        }
        from("${rootProject.projectDir}/zsh") {
          into("zsh")
        }
        from("${rootProject.projectDir}/bash") {
          into("bash")
        }
        into("bin") {
          from(nativeImage)
        }
      }
    }
  }
}
