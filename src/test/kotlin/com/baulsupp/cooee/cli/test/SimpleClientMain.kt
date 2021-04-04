package com.baulsupp.cooee.cli.test

import com.baulsupp.cooee.sdk.SimpleClient

suspend fun main() {
  val client = SimpleClient()
  client.run("github")
}
