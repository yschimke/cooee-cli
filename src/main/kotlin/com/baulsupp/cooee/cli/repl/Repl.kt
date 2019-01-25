package com.baulsupp.cooee.cli.repl

import com.baulsupp.cooee.cli.Main
import kotlinx.coroutines.runBlocking
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

class Repl(val tool: Main, val apiHost: String) {
  fun run() {
    val terminal = TerminalBuilder.builder()
      .system(true)
      .signalHandler(Terminal.SignalHandler.SIG_IGN)
      .name("coo.ee")
      .build()

    val reader = LineReaderBuilder.builder().completer(CooeeCompleter(tool, apiHost)).build()
    val prompt = "cooee $ "
    while (true) {
      try {
        val line = reader.readLine(prompt)
        runBlocking {
          runCommand(line)
        }
      } catch (e: UserInterruptException) {
        // Ignore
      } catch (e: EndOfFileException) {
        break
      }
    }
  }

  private suspend fun runCommand(line: String) {
    tool.cooeeCommand(line.split("\\s+"))
  }
}