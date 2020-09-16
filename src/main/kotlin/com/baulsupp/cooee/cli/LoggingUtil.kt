package com.baulsupp.cooee.cli

import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger

object OneLineLogFormat : Formatter() {
  private val d = ISO_LOCAL_TIME

  private val offset = ZoneOffset.systemDefault()

  override fun format(record: LogRecord): String {
    val message = formatMessage(record)

    val time = Instant.ofEpochMilli(record.millis).atZone(offset)

    return if (record.thrown != null) {
      val sw = StringWriter(4096)
      val pw = PrintWriter(sw)
      record.thrown.printStackTrace(pw)
      String.format("%s\t%s%n%s%n",d.format(time), message, sw.toString())
    } else {
      String.format("%s\t%s%n", d.format(time), message)
    }
  }
}

class LoggingUtil {
  companion object {
    private val activeLoggers = mutableListOf<Logger>()

    fun configureLogging(debug: Boolean) {
      if (debug) {
        LogManager.getLogManager().reset()
        val handler = ConsoleHandler()

          handler.level = Level.ALL
          handler.formatter = OneLineLogFormat
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
