package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.CommandResponse
import com.baulsupp.cooee.sdk.SimpleServer
import kotlinx.coroutines.flow.flowOf

fun main() {
  val server = SimpleServer(debug = false, local = true)

  server.exportSimpleCommands("hello", completer = { command ->
    listOf("yuri", "world")
  }) {
    flowOf(CommandResponse(message = "Hello " + it.parsed_command))
  }
}
