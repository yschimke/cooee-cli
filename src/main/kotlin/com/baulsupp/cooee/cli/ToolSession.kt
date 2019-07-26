package com.baulsupp.cooee.cli

import com.baulsupp.oksocial.output.OutputHandler
import okhttp3.OkHttpClient
import okhttp3.Response

interface ToolSession {
  fun close()

  var client: OkHttpClient
  var outputHandler: OutputHandler<Response>
}
