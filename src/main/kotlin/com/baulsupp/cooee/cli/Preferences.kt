package com.baulsupp.cooee.cli

import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.logging.Level
import java.util.logging.Logger

data class Preferences(
  val api: String? = null
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
