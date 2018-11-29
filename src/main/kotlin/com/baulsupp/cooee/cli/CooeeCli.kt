package com.baulsupp.cooee.cli

import com.github.rvesse.airline.HelpOption
import com.github.rvesse.airline.SingleCommand
import com.github.rvesse.airline.annotations.Arguments
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import com.github.rvesse.airline.help.Help
import com.github.rvesse.airline.parser.errors.ParseException
import java.util.ArrayList
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * Simple command line tool to make a RSocket connection and send/receive elements.
 *
 * Currently limited in features, only supports a text/line based approach.
 */
@Command(name = "cooee-cli", description = "CLI for Coo.ee")
class Main {
  @Inject
  var help: HelpOption<Main>? = null

  @Option(name = ["-H", "--header"], description = "Custom header to pass to server")
  var headers: List<String>? = null

  @Arguments(title = ["arguments"], description = "Remote resource URLs")
  var arguments: MutableList<String> = ArrayList()

  fun run(): Int {
    if (help?.showHelpIfRequested() == true) {
      return 0
    }

    println(arguments)

    return 0
  }
}

private fun fromArgs(vararg args: String): Main {
  val cmd = SingleCommand.singleCommand<Main>(Main::class.java)
  return try {
    cmd.parse(*args)
  } catch (e: ParseException) {
    System.err.println(e.message)
    Help.help(cmd.commandMetadata)
    exitProcess(-1)
  }
}

suspend fun main(vararg args: String) {
  fromArgs(*args).run()
}