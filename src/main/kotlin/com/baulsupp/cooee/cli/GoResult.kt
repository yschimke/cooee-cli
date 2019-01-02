package com.baulsupp.cooee.cli

data class GoResult(val location: String?, val message: String?, val image: String?)
data class CompletionResult(val completions: List<String>)
