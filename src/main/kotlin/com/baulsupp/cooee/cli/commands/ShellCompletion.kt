package com.baulsupp.cooee.cli.commands

import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.p.CompletionRequest
import com.baulsupp.cooee.p.CompletionResponse

enum class Shell {
  BASH, ZSH
}

suspend fun Main.completeCommand(line: String) =
  rsocketClient.requestResponse<CompletionRequest, CompletionResponse>("complete", CompletionRequest(line = line))

suspend fun Main.showCompletions(line: String, shell: Shell) {
  val completionList = completeCommand(line)

  when (shell) {
    Shell.ZSH -> outputHandler.info(completionList.completions.mapNotNull { "" + it.word + ":'${it.command?.description ?: it.line}'" }.joinToString("\n"))
    Shell.BASH -> outputHandler.info(completionList.completions.mapNotNull { it.word }.joinToString("\n"))
  }
}
