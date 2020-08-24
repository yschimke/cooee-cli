package com.baulsupp.cooee.cli

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Response

suspend fun Main.todoCommand() = coroutineScope {
  val queryResult = todoQuery()

  for (result in queryResult.todos) {
    outputHandler.info(result.line + ":" + result.message)
    if (result.url != null) {
      outputHandler.info(result.url)
    }
    outputHandler.info("")
  }
}

suspend fun Main.cooeeCommand(runArguments: List<String>): Int = coroutineScope {
  val result = bounceQuery(runArguments)

  if (result.location != null || result.message != null || result.image != null) {
    var imageResponse: Deferred<Response>? = null
    if (result.image != null) {
      imageResponse = async { client.execute(request(result.image)) }
    }
    if (result.message != null) {
      outputHandler.info(result.message)
    }
    if (result.location != null) {
      @Suppress("EXPERIMENTAL_API_USAGE")
      launch(start = CoroutineStart.ATOMIC) {
        outputHandler.openLink(result.location)
      }
    }
    if (imageResponse != null) {
      outputHandler.showOutput(imageResponse.await())
    }
    0
  } else {
    outputHandler.showError("No results found")
    -1
  }
}

suspend fun Main.bounceQuery(runArguments: List<String>) =
  client.query<GoResult>("${apiHost()}/api/v0/goinfo?q=${runArguments.joinToString(" ")}")

suspend fun Main.todoQuery() =
  client.query<TodoResult>("${apiHost()}/api/v0/todo")
