package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.TodoRequest
import com.baulsupp.cooee.p.TodoResponse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect

fun Main.todoQuery() =
  rsocketClient.requestStream<TodoRequest, TodoResponse>("todo", TodoRequest())

suspend fun Main.showTodos() = coroutineScope {
  val queryResult = todoQuery()

  queryResult.collect {
    for (result in it.todos) {
      outputHandler.info(result.command + ": " + result.message)
      if (result.url != null) {
        outputHandler.info(result.url)
      }
      outputHandler.info("")
    }
  }
}
