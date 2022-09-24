package com.baulsupp.cooee.cli.test

suspend fun main() {
  val client = SimpleClient()
  client.run("welcome")
  client.main.close()
}
