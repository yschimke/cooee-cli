package com.baulsupp.cooee.cli

import okhttp3.internal.http2.Http2
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class LoggingUtil {
  companion object {
    private val activeLoggers = mutableListOf<Logger>()

    fun configureLogging(debug: Boolean) {
      if (debug) {
        LogManager.getLogManager().reset()
        val handler = ConsoleHandler()

          handler.level = Level.ALL
          handler.formatter = OneLineLogFormat()
          val activeLogger = getLogger("")
          activeLogger.addHandler(handler)
          activeLogger.level = Level.ALL
      } else {
        getLogger("com.launchdarkly.eventsource").level = Level.SEVERE
      }
    }

    fun getLogger(name: String): Logger {
      val logger = Logger.getLogger(name)
      activeLoggers.add(logger)
      return logger
    }
  }
}
