package com.baulsupp.cooee.cli.test

import com.baulsupp.cooee.p.CommandRequest
import com.baulsupp.cooee.p.CommandResponse
import com.baulsupp.cooee.p.CompletionResponse
import com.baulsupp.cooee.p.CompletionSuggestion
import com.baulsupp.cooee.sdk.SimpleServer
import kotlinx.coroutines.flow.flowOf

suspend fun main() {
  val server = SimpleServer(debug = false, local = true)
  server.command = "welcome"
  server.completionHandler = { request ->
    println("complete: $request")
    CompletionResponse(listOf(CompletionSuggestion(word = "welcome")))
  }
  server.commandHandler = { request: CommandRequest ->
    println("ccommand: $request")
    flowOf(CommandResponse(message = "Welcome"))
  }
  server.start()
}
