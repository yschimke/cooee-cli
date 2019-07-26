package com.baulsupp.cooee.cli

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val JSON = "application/json".toMediaType()

fun Request.edit(init: Request.Builder.() -> Unit = {}) = newBuilder().apply(init).build()
fun HttpUrl.edit(init: HttpUrl.Builder.() -> Unit = {}) = newBuilder().apply(init).build()

object Rfc3339InstantJsonAdapter : JsonAdapter<Instant>() {
  override fun fromJson(reader: JsonReader): Instant = Instant.parse(reader.nextString())

  override fun toJson(writer: JsonWriter, value: Instant?) {
    writer.value(value?.toString())
  }
}

val moshi = Moshi.Builder()
  .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
  .add(Instant::class.java, Rfc3339InstantJsonAdapter.nullSafe())
  .build()!!

suspend inline fun <reified T> OkHttpClient.query(url: String): T {
  return this.query(request(url))
}

suspend inline fun <reified T> OkHttpClient.query(request: Request): T {
  val stringResult = this.queryForString(request)

  return moshi.adapter(T::class.java).fromJson(stringResult)!!
}

suspend fun OkHttpClient.queryForString(request: Request): String = execute(request).body!!.string()

suspend fun OkHttpClient.queryForString(url: String): String =
  this.queryForString(request(url))

suspend fun OkHttpClient.execute(request: Request): Response {
  val call = this.newCall(request)

  val response = call.await()

  if (!response.isSuccessful) {
    val responseString = response.body!!.string()

    val msg: String = if (responseString.isNotEmpty()) {
      responseString
    } else {
      response.statusMessage()
    }

    throw ClientException(msg, response.code)
  }

  return response
}

fun Response.statusMessage(): String = this.code.toString() + " " + this.message

suspend fun Call.await(): Response {
  return suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation {
      cancel()
    }
    enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        if (!cont.isCompleted) {
          cont.resumeWithException(e)
        }
      }

      override fun onResponse(call: Call, response: Response) {
        if (!cont.isCompleted) {
          cont.resume(response)
        }
      }
    })
  }
}

class ClientException(val responseMessage: String, val code: Int) : IOException("$code: $responseMessage")


fun request(
  url: String? = null,
  init: Request.Builder.() -> Unit = {}
): Request = requestBuilder(url).apply(init).build()

fun requestBuilder(
  url: String? = null
): Request.Builder = Request.Builder().apply { if (url != null) url(url) }
