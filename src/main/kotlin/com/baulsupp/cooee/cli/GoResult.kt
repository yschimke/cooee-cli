package com.baulsupp.cooee.cli

data class Suggestion(
  val line: String,
  val provider: String,
  val description: String,
  val type: String,
  val list: List<Suggestion>? = null,
  val url: String? = null,
  val message: String? = null
) {
  fun startsWith(command: String): Boolean {
    return line.startsWith(command)
  }

  fun contains(command: String): Boolean {
    return line.contains(command)
  }
}

data class GoResult(val location: String?, val message: String?, val image: String?)
data class CompletionItem(val word: String, val line: String, val description: String, val provider: String)
data class CompletionResult(val completions: List<CompletionItem>?, val suggestions: List<Suggestion>?)
data class TodoResult(val todos: List<Suggestion>)
