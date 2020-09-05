package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.CommandRequest
import com.baulsupp.cooee.p.CommandResponse
import com.baulsupp.cooee.p.CommandStatus
import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.authenticator.oauth2.Oauth2Token
import com.baulsupp.okurl.credentials.TokenValue
import kotlinx.coroutines.*
import okhttp3.Response

suspend fun Main.cooeeCommand(openExtraLinks: Boolean, runArguments: List<String>): Int = coroutineScope {
  val result = runCommand(runArguments)

  val imageUrl = result.image_url?.url
  if (result.location != null || result.message != null || imageUrl != null) {
    var imageResponse: Deferred<Response?>? = null
    if (imageUrl != null) {
      imageResponse = async {
        try {
          client.execute(request(imageUrl, tokenSet = TokenValue(Oauth2Token("pk.eyJ1IjoieXNjaGlta2UiLCJhIjoiY2tlb3E5MWIyMWp4eDJ2azdraWg5cHkxYyJ9.UHmWRzY_VE7gqIjCwIAmNA"))))
        } catch (ce: ClientException) {
          if (ce.code == 404) {
            throw UsageException("image not found: $imageUrl")
            null
          } else if (ce.code == 401) {
              throw UsageException("unauthorised: $imageUrl")
              null
          } else {
            throw ce
          }
        }
      }
    }
    if (result.message != null) {
      outputHandler.info(result.message)
    }
    if (result.location != null && (result.status == CommandStatus.REDIRECT || openExtraLinks)) {
      @Suppress("EXPERIMENTAL_API_USAGE")
      launch(start = CoroutineStart.ATOMIC) {
        outputHandler.openLink(result.location)
      }
    }
    if (imageResponse != null) {
      val response = imageResponse.await()

      if (response != null) {
        outputHandler.showOutput(response)
      }
    }

    0
  } else {
    outputHandler.showError("No results found")
    -1
  }
}

suspend fun Main.runCommand(runArguments: List<String>) =
  rsocketClient.requestResponse<CommandRequest, CommandResponse>("runCommand", CommandRequest(parsed_command = runArguments))