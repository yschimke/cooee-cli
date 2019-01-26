package com.baulsupp.cooee.cli.repl

import com.baulsupp.cooee.cli.Main
import com.baulsupp.okurl.credentials.TokenValue
import com.baulsupp.okurl.kotlin.query
import kotlinx.coroutines.runBlocking
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import java.io.File

data class UserInfo(val email: String, val name: String)

class Repl(val tool: Main, val apiHost: String) {
  fun run() {
    val terminal = TerminalBuilder.builder()
      .system(true)
      .signalHandler(Terminal.SignalHandler.SIG_IGN)
      .name("cooee")
      .build()

    val history = DefaultHistory()

    Runtime.getRuntime().addShutdownHook(Thread {
      history.save()
    })

    val reader =
      LineReaderBuilder.builder().terminal(terminal).history(history).completer(CooeeCompleter(tool, apiHost)).variable(
        LineReader.HISTORY_FILE, File(System.getenv("HOME"), ".cooee/repl.hist")
      ).build()
    history.attach(reader)

    val prompt = buildPrompt()
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

  private fun buildPrompt(): String? {
    val user = runBlocking {
      tool.client.query<UserInfo>("https://api.coo.ee/api/v0/user").email
    }
    val todos = 0

    return AttributedStringBuilder()
      .style(AttributedStyle.DEFAULT)
      .append("coo.ee [")
      .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
      .append("$user:")
      .style(AttributedStyle.DEFAULT)
      .append("$todos]")
      .style(AttributedStyle.BOLD)
      .append(" $ ")
      .style(AttributedStyle.DEFAULT)
      .toAnsi()
  };

  private suspend fun runCommand(line: String) {
    tool.cooeeCommand(line.split("\\s+"))
  }
}
