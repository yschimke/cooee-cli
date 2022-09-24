import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
  kotlin("jvm") version "1.7.10"
  kotlin("kapt") version "1.7.10"
  `maven-publish`
  application
  id("net.nemerosa.versioning") version "2.15.1"
  id("com.diffplug.spotless") version "6.11.0"
  id("com.palantir.graal") version "0.12.0"
  id("com.squareup.wire") version "4.4.1"
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
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

tasks {
  withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "17"
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
  graalVersion("22.2.0")
  javaVersion("17")

  option("--enable-https")
  option("--allow-incomplete-classpath")
  option("--no-fallback")
}

dependencies {
  implementation("info.picocli:picocli:4.6.3")
  api("com.squareup.moshi:moshi:1.14.0")
  api("com.squareup.moshi:moshi-adapters:1.14.0")
  api("com.squareup.moshi:moshi-kotlin:1.14.0")
  api("com.squareup.okhttp3:okhttp:5.0.0-alpha.10")
  api("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.10")
  api("com.squareup.okhttp3:okhttp-brotli:5.0.0-alpha.10")
  api("com.squareup.okhttp3:okhttp-tls:5.0.0-alpha.10")
  api("com.squareup.okhttp3:okhttp-sse:5.0.0-alpha.10")
  api("com.squareup.okhttp3:okhttp-dnsoverhttps:5.0.0-alpha.10")
  api("com.github.yschimke.schoutput:schoutput:0.9.2")
  implementation("io.jsonwebtoken:jjwt-api:0.11.5")
  implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
  api("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
  implementation("org.slf4j:slf4j-jdk14:2.0.0")
  implementation("com.github.yschimke:okurl:v4.3.0") {
    isTransitive = false
  }

  kapt("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
  kapt("info.picocli:picocli-codegen:4.6.3")
  implementation("org.graalvm.nativeimage:svm:22.2.0") {
    // https://youtrack.jetbrains.com/issue/KT-29513
    exclude(group= "org.graalvm.nativeimage")
    exclude(group= "org.graalvm.truffle")
//    exclude(group= "org.graalvm.sdk")
    exclude(group= "org.graalvm.compiler")
  }
  implementation("io.github.classgraph:classgraph:4.8.149")

  implementation("io.swagger.parser.v3:swagger-parser:2.1.2")

  implementation("io.rsocket.kotlin:rsocket-core-jvm:0.15.4")
  implementation("io.ktor:ktor-network-tls:2.1.1")
  implementation("io.ktor:ktor-client-okhttp:2.1.1") {
    exclude(group= "com.squareup.okhttp3")
  }
  implementation("io.ktor:ktor-client-core-jvm:2.1.1") {
    exclude(group= "com.squareup.okhttp3")
  }
  implementation("io.rsocket.kotlin:rsocket-ktor-client:0.15.4")

  implementation("com.squareup.wire:wire-runtime:4.4.1")
  implementation("com.squareup.wire:wire-moshi-adapter:4.4.1")
  implementation("com.squareup.wire:wire-grpc-client:4.4.1") {
    exclude(group= "com.squareup.okhttp3")
  }
  implementation("com.squareup.wire:wire-moshi-adapter:4.4.1")

  testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.10")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
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
