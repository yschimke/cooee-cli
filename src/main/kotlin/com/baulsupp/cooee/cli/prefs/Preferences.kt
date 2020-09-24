package com.baulsupp.cooee.cli.prefs

import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.cli.util.moshi
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

@JsonClass(generateAdapter = true)
data class Preferences(
  val api: String = "wss://stream.coo.ee/rsocket",
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
