package com.baulsupp.cooee.cli.test

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
suspend fun main(args: Array<String>) {
  val testArgs = "--command-completion"
  com.baulsupp.cooee.cli.main(*testArgs.split(" ").toTypedArray())
}
