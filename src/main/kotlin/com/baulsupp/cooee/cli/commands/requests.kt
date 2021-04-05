package com.baulsupp.cooee.cli.commands

import com.baulsupp.cooee.cli.util.moshi
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.ExperimentalMetadataApi
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.metadata.CompositeMetadata
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.toPacket
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMetadataApi::class)
fun buildMetadata(route: String): ByteReadPacket {
  return CompositeMetadata(RoutingMetadata(route)).toPacket()
}

suspend inline fun <reified Request, reified Response> RSocket.requestResponse(route: String, request: Request): Response {
  val requestAdapter = moshi.adapter(Request::class.java)
  val requestPayload = buildPayload {
    data(requestAdapter.toJson(request))
    metadata(buildMetadata(route))
  }

  val responsePayload = requestResponse(requestPayload)

  val readText = responsePayload.data.readText()
  val responseAdapter = moshi.adapter(Response::class.java)
  return responseAdapter.fromJson(readText) ?: throw IllegalStateException("Null response")
}

inline fun <reified Request, reified Response> RSocket.requestStream(route: String, request: Request): Flow<Response> {
  val requestAdapter = moshi.adapter(Request::class.java)
  val requestPayload = buildPayload {
    data(requestAdapter.toJson(request))
    metadata(buildMetadata(route))
  }

  val responseAdapter = moshi.adapter(Response::class.java)
  return requestStream(requestPayload).map {
    responseAdapter.fromJson(it.data.readText()) ?: throw IllegalStateException("Null response")
  }
}
