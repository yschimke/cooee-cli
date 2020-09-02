package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.TodoRequest
import com.baulsupp.cooee.p.TodoResponse
import kotlinx.coroutines.coroutineScope

suspend fun Main.todoQuery() =
  rsocketClient.requestResponse<TodoRequest, TodoResponse>("todo", TodoRequest())

suspend fun Main.showTodos() = coroutineScope {
  val queryResult = todoQuery()

  for (result in queryResult.todos) {
    outputHandler.info(result.command + ": " + result.message)
    if (result.url != null) {
      outputHandler.info(result.url)
    }
    outputHandler.info("")
  }
}
