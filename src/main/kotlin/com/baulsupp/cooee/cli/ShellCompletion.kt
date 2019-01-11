package com.baulsupp.cooee.cli

import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.commands.ToolSession
import com.baulsupp.okurl.kotlin.query
import java.util.logging.Level
import java.util.logging.Logger

enum class Shell {
  BASH, FISH
}

class ShellCompletion(val tool: ToolSession, val apiHost: String, val shell: Shell) {
  suspend fun completeCommand(line: String) {
    try {
      val completionList: CompletionResult = completionQuery(line)
      tool.outputHandler.info(completionList.completions.joinToString("\n") {
        when (shell) {
          Shell.FISH -> "${it.line}\t${it.description}"
          else -> it.word
        }
      })
    } catch (ue: UsageException) {
      throw ue
    } catch (e: Exception) {
      logger.log(Level.FINE, "failure during url completion", e)
    }
  }

  suspend fun completionQuery(query: String) =
    tool.client.query<CompletionResult>("${apiHost}/api/v0/completion?q=${query.replace(" ", "+")}")

  companion object {
    private val logger = Logger.getLogger(ShellCompletion::class.java.name)
  }
}

