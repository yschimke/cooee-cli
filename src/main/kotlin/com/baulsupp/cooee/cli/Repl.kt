package com.baulsupp.cooee.cli

import kotlinx.coroutines.runBlocking
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.Candidate
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.MaskingCallback
import org.jline.reader.ParsedLine
import org.jline.reader.Parser
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
suspend fun main() {
  AnsiConsole.systemInstall();

  val main = Main().apply {
    initialise()
  }

  val parser: Parser = DefaultParser()
  val terminal: Terminal = TerminalBuilder.builder().build()

  val reader: LineReader = LineReaderBuilder.builder()
    .terminal(terminal)
    .completer { lineReader: LineReader, parsedLine: ParsedLine, mutableList: MutableList<Candidate> ->
      runBlocking {
        val completions = main.completeCommand(parsedLine.line())
        val candidates = completions.completions.map { Candidate(it.word, it.word, it.command?.provider, it.command?.description, null, it.word, it.command != null) }
        mutableList.addAll(candidates)
      }
    }
    .parser(parser)
    .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
    .build()

  val prompt = "cooee> "
  val rightPrompt: String? = null

  // start the shell and process input until the user quits with Ctrl-D
  var line: String?
  while (true) {
    try {
      line = reader.readLine(prompt, rightPrompt, null as MaskingCallback?, null)

      main.cooeeCommand(false, reader.parsedLine.words())
    } catch (e: UserInterruptException) {
      // Ignore
    } catch (e: EndOfFileException) {
      return
    }
  }
}
