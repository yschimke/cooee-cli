package com.baulsupp.cooee.cli.test

import com.baulsupp.cooee.cli.Main

fun main() {
  val testArgs = "github pulls"
  Main.main(*testArgs.split(" ").toTypedArray())
}
