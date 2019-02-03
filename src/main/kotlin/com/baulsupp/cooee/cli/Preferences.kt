package com.baulsupp.cooee.cli

import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import java.io.File
import java.net.InetSocketAddress
import java.util.logging.Level
import java.util.logging.Logger

data class Proxy(val host: String? = null, val port: Int = 8080, val user: String? = null, val password: String? = null, val type: java.net.Proxy.Type = java.net.Proxy.Type.DIRECT) {
  fun build(): java.net.Proxy {
    val address = if (host != null) InetSocketAddress(host, port) else null

    return when (type) {
      java.net.Proxy.Type.DIRECT -> java.net.Proxy.NO_PROXY
      else -> java.net.Proxy(type, address)
    }
  }
}

data class Preferences(
  val api: String = "https://api.coo.ee",
  val web: String = "https://www.coo.ee",
  val proxy: Proxy = Proxy(),
  val descriptionLength: Int = 20
) {
  companion object {
    private val logger = Logger.getLogger(Preferences::class.java.name)

    val local: Preferences by lazy {
      val prefFile = File(System.getenv("HOME"), ".cooee/prefs.json")

      if (prefFile.exists()) {
        try {
          val moshi = Moshi.Builder().build()
          moshi.adapter(Preferences::class.java).fromJson(prefFile.source().buffer()) ?: Preferences()
        } catch (e: Exception) {
          logger.log(Level.FINE, "failed loading preferences", e)
          Preferences()
        }
      } else {
        Preferences()
      }
    }
  }
}
