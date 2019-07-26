package com.baulsupp.cooee.cli

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class GoResult(val location: String?, val message: String?, val image: String?)

@JsonClass(generateAdapter = true)
data class CompletionItem(val word: String, val line: String, val description: String, val provider: String)

@JsonClass(generateAdapter = true)
data class CompletionResult(val completions: List<CompletionItem>?, val suggestions: List<Suggestion>?)

@JsonClass(generateAdapter = true)
data class TodoResult(val todos: List<Suggestion>)
