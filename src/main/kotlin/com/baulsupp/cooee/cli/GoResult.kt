package com.baulsupp.cooee.cli

data class GoResult(val location: String?, val message: String?, val image: String?)
data class CompletionItem(val word: String, val line: String, val description: String)
data class CompletionResult(val completions: List<CompletionItem>)
