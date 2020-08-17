package com.baulsupp.cooee.cli.test

import com.baulsupp.cooee.cli.Main

fun main() {
  val testArgs = "--login"
  Main.main(*testArgs.split(" ").toTypedArray())
}
