package com.baulsupp.cooee.cli

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.CompositeByteBuf
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.payload.Payload
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

inline fun <reified Request, reified Response> RSocket.requestStream(route: String, request: Request): Flow<Response> {
  val requestAdapter = moshi.adapter(Request::class.java)
  val responseAdapter = moshi.adapter(Response::class.java)

  val requestPayload = Payload(requestAdapter.toJson(request).toByteArray(), buildMetadata(route))

  return requestStream(requestPayload).map {
    responseAdapter.fromJson(it.data.readText()) ?: throw IllegalStateException("Null response")
  }
}
