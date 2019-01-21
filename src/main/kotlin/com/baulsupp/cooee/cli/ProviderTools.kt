package com.baulsupp.cooee.cli

import com.baulsupp.okurl.kotlin.JSON
import com.baulsupp.okurl.kotlin.execute
import com.baulsupp.okurl.kotlin.moshi
import com.baulsupp.okurl.kotlin.query
import com.baulsupp.okurl.kotlin.request
import okhttp3.OkHttpClient
import okhttp3.RequestBody

data class ProviderList(val providers: List<ProviderStatus>)
data class ProviderStatus(
  val name: String,
  val installed: Boolean,
  val config: Map<String, Any>?,
  val services: List<String>
)

data class ProviderRequest(
  val config: Map<String, Any>?
)

class ProviderTools(val client: OkHttpClient) {
  suspend fun list(): List<ProviderStatus> = client.query<ProviderList>("https://api.coo.ee/api/v0/providers").providers

  suspend fun remove(name: String) {
    client.execute(request("https://api.coo.ee/api/v0/provider/$name") {
      method("DELETE", null)
    })
  }

  suspend fun add(name: String, request: ProviderRequest) {
    client.execute(request("https://api.coo.ee/api/v0/provider/$name") {
      val content = moshi.adapter(ProviderRequest::class.java).toJson(request)!!
      method("PUT", RequestBody.create(JSON, content))
    })
  }
}
