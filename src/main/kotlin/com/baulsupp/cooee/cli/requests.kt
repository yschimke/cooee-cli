package com.baulsupp.cooee.cli

import com.baulsupp.cooee.p.CommandRequest
import com.baulsupp.cooee.p.CommandResponse
import com.baulsupp.cooee.p.TodoRequest
import com.baulsupp.cooee.p.TodoResponse
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.CompositeByteBuf
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.payload.Payload
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Response
import java.lang.IllegalStateException

suspend fun Main.cooeeCommand(runArguments: List<String>): Int = coroutineScope {
  val result = bounceQuery(runArguments)

  val imageUrl = result.image_url?.url
  if (result.location != null || result.message != null || imageUrl != null) {
    var imageResponse: Deferred<Response?>? = null
    if (imageUrl != null) {
      imageResponse = async {
        try {
          client.execute(request(imageUrl))
        } catch (ce: ClientException) {
          if (ce.code == 404) {
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
    if (result.location != null) {
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

suspend fun Main.bounceQuery(runArguments: List<String>) =
  rsocketClient.requestResponse<CommandRequest, CommandResponse>("runCommand", CommandRequest(parsed_command = runArguments))

fun buildMetadata(route: String): ByteArray? {
  val compositeByteBuf = CompositeByteBuf(ByteBufAllocator.DEFAULT, false, 1)
  val routingMetadata = TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, listOf(route))
  CompositeMetadataCodec.encodeAndAddMetadata(compositeByteBuf, ByteBufAllocator.DEFAULT,
    WellKnownMimeType.MESSAGE_RSOCKET_ROUTING, routingMetadata.content)
  return ByteBufUtil.getBytes(compositeByteBuf)
}

suspend inline fun <reified Request, reified Response> RSocket.requestResponse(route: String, request: Request): Response {
  val requestAdapter = moshi.adapter(Request::class.java)
  val responseAdapter = moshi.adapter(Response::class.java)

  val requestPayload = Payload(requestAdapter.toJson(request).toByteArray(), buildMetadata(route))

  val responsePayload = requestResponse(requestPayload)

  return responseAdapter.fromJson(responsePayload.data.readText()) ?: throw IllegalStateException("Null response")
}
