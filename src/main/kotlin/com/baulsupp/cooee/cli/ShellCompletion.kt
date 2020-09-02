package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.CompletionRequest
import com.baulsupp.cooee.p.CompletionResponse

suspend fun Main.completeCommand(word: String, line: String) =
  rsocketClient.requestResponse<CompletionRequest, CompletionResponse>("complete", CompletionRequest(word = word, line = line))

suspend fun Main.showCompletions(word: String, line: String) {
  val completionList = completeCommand(word, line)

  outputHandler.info(completionList.completions.mapNotNull { it.line }.joinToString("\n"))
}
