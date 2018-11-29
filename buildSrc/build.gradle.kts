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
  maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/")
}
