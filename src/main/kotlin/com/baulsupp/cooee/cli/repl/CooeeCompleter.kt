package com.baulsupp.cooee.cli.repl

import com.baulsupp.cooee.cli.CompletionResult
import com.baulsupp.cooee.cli.Main
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
        candidates.add(Candidate(it.line, it.line, it.provider, it.description, null, null, true))
      }
    }
  }
}
