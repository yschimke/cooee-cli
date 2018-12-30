package com.baulsupp.cooee.cli

data class GoResult(val location: String?, val message: String?)
data class CompletionResult(val completions: List<String>)
