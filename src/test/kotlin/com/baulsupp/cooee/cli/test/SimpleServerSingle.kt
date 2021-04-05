package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.CommandResponse
import com.baulsupp.cooee.sdk.SimpleServer
import kotlinx.coroutines.flow.flowOf

fun main() {
  val server = SimpleServer(debug = false, local = true)

  server.exportSingleCommand("welcome") {
    flowOf(CommandResponse(message = "Welcome"))
  }
}
