package com.baulsupp.cooee.cli.commands

import com.baulsupp.cooee.cli.util.ClientException
import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.cli.util.execute
import com.baulsupp.cooee.cli.util.request
import com.baulsupp.cooee.p.CommandRequest
import com.baulsupp.cooee.p.CommandResponse
import com.baulsupp.cooee.p.CommandStatus
import com.baulsupp.cooee.p.Table
import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.authenticator.oauth2.Oauth2Token
import com.baulsupp.okurl.credentials.TokenValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import okhttp3.Response

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Main.cooeeCommand(openExtraLinks: Boolean, runArguments: List<String>): Int = coroutineScope {
  val resultFlow = runCommand(runArguments)

  resultFlow.collect { result ->
    val imageUrl = result.image_url?.url
    if (result.url != null || result.message != null || imageUrl != null || result.table != null) {
      var imageResponse: Deferred<Response?>? = null
      if (imageUrl != null) {
        imageResponse = async {
          try {
            client.execute(
              request(imageUrl, tokenSet = TokenValue(Oauth2Token("pk.eyJ1IjoieXNjaGlta2UiLCJhIjoiY2tlb3E5MWIyMWp4eDJ2azdraWg5cHkxYyJ9.UHmWRzY_VE7gqIjCwIAmNA"))))
          } catch (ce: ClientException) {
            when (ce.code) {
              404 -> throw UsageException("image not found: $imageUrl")
              401 -> throw UsageException("unauthorised: $imageUrl")
              else -> throw ce
            }
          }
        }
      }
      if (result.message != null) {
        outputHandler.info(result.message)
      }
      if (result.table != null) {
        outputHandler.info(tableToString(result.table))
      }
      if (result.url != null && (result.status == CommandStatus.REDIRECT || openExtraLinks)) {
        launch(start = CoroutineStart.ATOMIC) {
          outputHandler.openLink(result.url)
        }
      }
      if (imageResponse != null) {
        val response = imageResponse.await()

        if (response != null) {
          outputHandler.showOutput(response)
        }
      }
    }
  }

  0
}

fun tableToString(table: Table): String {
  val columns = table.columns
  return buildString {
    append(columns.map { it.name }.joinToString("\t"))

    columns.firstOrNull()?.values?.indices?.forEach { rowNum ->
      append("\n")
      append(columns.map { it.values[rowNum] }.joinToString("\t"))
    }
  }
//
//
//  val flipTable = table {
//    cellStyle {
//      border = false
//      alignment = TextAlignment.TopLeft
//      paddingRight = 1
//    }
//
//    header {
//      row(*columns.map { it.name }.toTypedArray())
//    }
//    columns.firstOrNull()?.values?.indices?.forEach { rowNum ->
//      row(*columns.map { it.values[rowNum] }.toTypedArray())
//    }
//  }
//
//  val tableString = flipTable.toString()
//  return tableString
}

//fun tableToString(table: Table): String {
//  val columns = table.columns
//
//  val flipTable = table {
//    cellStyle {
//      border = false
//      alignment = TextAlignment.TopLeft
//      paddingRight = 1
//    }
//
//    header {
//      row(*columns.map { it.name }.toTypedArray())
//    }
//    columns.firstOrNull()?.values?.indices?.forEach { rowNum ->
//      row(*columns.map { it.values[rowNum] }.toTypedArray())
//    }
//  }
//
//  val tableString = flipTable.toString()
//  return tableString
//}

suspend fun Main.runCommand(runArguments: List<String>) =
  rsocketClient.requestStream<CommandRequest, CommandResponse>("runCommand", CommandRequest(parsed_command = runArguments))
