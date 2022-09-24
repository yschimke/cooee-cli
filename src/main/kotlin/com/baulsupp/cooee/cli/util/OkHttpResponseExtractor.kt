package com.baulsupp.cooee.cli.util

import com.baulsupp.schoutput.responses.ResponseExtractor
import okhttp3.Response
import okio.BufferedSource

object OkHttpResponseExtractor : ResponseExtractor<Response> {
  override fun mimeType(response: Response): String? = response.body.contentType()?.toString()

  override fun source(response: Response): BufferedSource = response.body.source()

  override fun filename(response: Response): String {
    val segments = response.request.url.pathSegments

    return segments[segments.size - 1]
  }
}
