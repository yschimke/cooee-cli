package com.baulsupp.cooee.cli

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.net.InetSocketAddress
import java.util.logging.Level
import java.util.logging.Logger

data class Preferences(
  val api: String = "https://api.coo.ee",
  val web: String = "https://www.coo.ee",
  val descriptionLength: Int = 20
) {
  suspend fun save() {
    withContext(Dispatchers.IO) {
      prefFile.writeText(moshi.adapter(Preferences::class.java).toJson(this@Preferences))
    }
  }

  companion object {
    private val logger = Logger.getLogger(Preferences::class.java.name)
    val prefFile = File(Main.configDir, "prefs.json")

    val local: Preferences by lazy {
      if (prefFile.exists()) {
        try {
          moshi.adapter(Preferences::class.java).fromJson(prefFile.source().buffer()) ?: Preferences()
        } catch (e: Exception) {
          logger.log(Level.INFO, "failed loading preferences", e)
          Preferences()
        }
      } else {
        Preferences()
      }
    }
  }
}
