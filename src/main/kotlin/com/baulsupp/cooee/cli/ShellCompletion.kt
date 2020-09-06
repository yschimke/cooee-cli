package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.CompletionRequest
import com.baulsupp.cooee.p.CompletionResponse

enum class Shell {
  BASH, ZSH
}

suspend fun Main.completeCommand(word: String, line: String) =
  rsocketClient.requestResponse<CompletionRequest, CompletionResponse>("complete", CompletionRequest(word = word, line = line))

suspend fun Main.showCompletions(word: String, line: String, shell: Shell) {
  val completionList = completeCommand(word, line)

  when (shell) {
    Shell.ZSH -> outputHandler.info(completionList.completions.mapNotNull { it.word }.joinToString("\n"))
    Shell.BASH -> outputHandler.info(completionList.completions.mapNotNull { it.word }.joinToString("\n"))
  }
}
