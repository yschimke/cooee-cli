package com.baulsupp.cooee.cli.test

import com.baulsupp.cooee.cli.Main

fun main() {
//  println(ReflectionConfigGenerator.generateReflectionConfig(CommandLine.Model.CommandSpec.forAnnotatedObject(Main())))

  val testArgs = ""
  Main.main(*testArgs.split(" ").toTypedArray())
}
