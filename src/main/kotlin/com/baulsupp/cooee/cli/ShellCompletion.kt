package com.baulsupp.cooee.cli

import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.commands.ToolSession
import com.baulsupp.okurl.kotlin.query
import java.util.logging.Level
import java.util.logging.Logger

enum class Shell {
  BASH,
  FISH
}

class ShellCompletion(val tool: ToolSession, val apiHost: String, val shell: Shell) {
  suspend fun completeCommand(line: String) {
    try {
      if (line.isNullOrEmpty()) {
        val completionList: TodoResult = todoQuery()
        tool.outputHandler.info(completionList.todos.joinToString("\n") {
          when (shell) {
            Shell.FISH -> "${it.line}\t${it.description}"
            else -> it.line.split("\\s+".toRegex()).last()
          }
        })
      } else {
        val completionList: CompletionResult = completionQuery(line)
        if (completionList.completions != null) {
          tool.outputHandler.info(completionList.completions.joinToString("\n") {
            when (shell) {
              Shell.FISH -> "${it.line}\t${it.description}"
              else -> it.word
            }
          })
        } else {
          tool.outputHandler.info(completionList.suggestions!!.joinToString("\n") {
            when (shell) {
              Shell.FISH -> "${it.line}\t${it.description}"
              else -> it.line.split("\\s+".toRegex()).last()
            }
          })
        }
      }
    } catch (ue: UsageException) {
      throw ue
    } catch (e: Exception) {
      logger.log(Level.FINE, "failure during url completion", e)
    }
  }

  suspend fun todoQuery() =
    tool.client.query<TodoResult>("$apiHost/api/v0/todo")

  suspend fun completionQuery(query: String) =
    tool.client.query<CompletionResult>("$apiHost/api/v0/completion?q=${query.replace(" ", "+")}")

  companion object {
    private val logger = Logger.getLogger(ShellCompletion::class.java.name)
  }
}
