package com.baulsupp.cooee.cli.test

import com.baulsupp.cooee.cli.Main

fun main() {
  val testArgs = "yschimke/okurl"
  Main.main(*testArgs.split(" ").toTypedArray())
}
