plugins {
  `kotlin-dsl`
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

// Required since Gradle 4.10+.
repositories {
  jcenter()
  mavenCentral()
  maven(url = "https://jitpack.io")
  maven(url = "https://repo.maven.apache.org/maven2")
  maven(url = "https://repo.spring.io/milestone")
  maven(url = "https://repo.spring.io/release")
  maven(url = "https://dl.bintray.com/whyoleg/rsocket-kotlin")
}
