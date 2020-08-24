package com.baulsupp.cooee.cli

import com.baulsupp.cooee.cli.Main.Companion.logger
import com.baulsupp.oksocial.output.UsageException
import java.util.logging.Level
import java.util.logging.Logger

suspend fun Main.completeCommand(line: String) {
  try {
    if (line.isEmpty()) {
      val completionList: TodoResult = todoQuery()
      outputHandler.info(completionList.todos.joinToString("\n") {
        it.line.split("\\s+".toRegex()).last()
      })
    } else {
      val completionList: CompletionResult =
        client.query<CompletionResult>("${apiHost()}/api/v0/completion?q=${line.replace(" ", "+")}")
      if (completionList.completions != null) {
        outputHandler.info(completionList.completions.joinToString("\n") {
          it.word
        })
      } else {
        outputHandler.info(completionList.suggestions!!.joinToString("\n") {
          it.line.split("\\s+".toRegex()).last()
        })
      }
    }
  } catch (ue: UsageException) {
    throw ue
  } catch (e: Exception) {
    logger.log(Level.FINE, "failure during url completion", e)
  }
}

