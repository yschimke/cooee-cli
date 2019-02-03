package com.baulsupp.cooee.cli.repl

import com.baulsupp.cooee.cli.CompletionItem
import com.baulsupp.cooee.cli.CompletionResult
import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.cli.Preferences
import com.baulsupp.okurl.kotlin.query
import kotlinx.coroutines.runBlocking
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class CooeeCompleter(val tool: Main, val apiHost: String) : Completer {
  suspend fun completionQuery(query: String) =
    tool.client.query<CompletionResult>("$apiHost/api/v0/completion?q=${query.replace(" ", "+")}")

  override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
    runBlocking {
      val completions = completionQuery(line.line())

      completions.completions.forEach {
        candidates.add(Candidate(it.word, it.line, it.provider, truncateDescription(it), null, null, true))
      }
    }
  }

  private fun truncateDescription(it: CompletionItem): String {
    val maxDescription = Preferences.local.descriptionLength
    return when {
      maxDescription >= 0 && it.description.length > maxDescription -> it.description.substring(0, maxDescription)
      else -> it.description
    }
  }
}

suspend fun main() {
  val cli = Main()
  cli.initialise()

  val completer = CooeeCompleter(cli, "http://localhost:8080")
  val completions = completer.completionQuery("cooee ")

  println(completions)
}
