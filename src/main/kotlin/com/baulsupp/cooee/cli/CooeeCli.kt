package com.baulsupp.cooee.cli

import com.baulsupp.oksocial.output.ConsoleHandler
import com.baulsupp.oksocial.output.OutputHandler
import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.kotlin.query
import com.github.rvesse.airline.HelpOption
import com.github.rvesse.airline.SingleCommand
import com.github.rvesse.airline.annotations.Arguments
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import com.github.rvesse.airline.help.Help
import com.github.rvesse.airline.parser.errors.ParseException
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.Closeable
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger
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

  @Option(name = ["-V", "--version"], description = "Show version number and quit")
  var version = false

  @Option(name = ["-l", "--local"], description = "Use local server")
  var local = false

  @Arguments(title = ["arguments"], description = "Remote resource URLs")
  var arguments: MutableList<String> = ArrayList()

  lateinit var client: OkHttpClient

  lateinit var outputHandler: OutputHandler<Response>

  val closeables = mutableListOf<Closeable>()

  open fun initialise() {
    System.setProperty("apple.awt.UIElement", "true")

    if (!this::outputHandler.isInitialized) {
      outputHandler = buildHandler()
    }

    closeables.add(Closeable {
      if (this::client.isInitialized) {
        client.dispatcher().executorService().shutdown()
        client.connectionPool().evictAll()
      }
    })

    if (!this::client.isInitialized) {
      val clientBuilder = createClientBuilder()

      client = clientBuilder.build()
    }
  }

  open fun buildHandler(): OutputHandler<Response> {
    return ConsoleHandler.instance()
  }

  private fun createClientBuilder(): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()

    return builder
  }

  open suspend fun runCommand(runArguments: List<String>): Int {
    val host = if (local) "http://localhost:8080" else "https://coo.ee"
    val result = client.query<GoResult>("$host/api/v0/goinfo?q=" + runArguments.joinToString(" "))

    if (result.location != null) {
      outputHandler.openLink(result.location)
      return 0
    } else {
      outputHandler.showError("No results found")
      return -1
    }
  }

  fun versionString(): String {
    return this.javaClass.`package`.implementationVersion ?: "dev"
  }

  suspend fun run(): Int {
    if (help?.showHelpIfRequested() == true) {
      return 0
    }

    initialise()

    if (version) {
      outputHandler.info("cooee-cli " + versionString())
      return 0
    }

    return try {
      runCommand(arguments)
    } catch (e: UsageException) {
      outputHandler.showError(e.message)
      -1
    } catch (e: Exception) {
      outputHandler.showError("unknown error", e)
      -2
    } finally {
      closeClients()
    }
  }

  private fun closeClients() {
    for (c in closeables) {
      try {
        c.close()
      } catch (e: Exception) {
        Logger.getLogger("main").log(Level.FINE, "close failed", e)
      }
    }
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
