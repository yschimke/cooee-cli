package com.baulsupp.cooee.cli

import com.baulsupp.oksocial.output.ConsoleHandler
import com.baulsupp.oksocial.output.OutputHandler
import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.authenticator.SimpleWebServer
import com.baulsupp.okurl.credentials.CredentialFactory
import com.baulsupp.okurl.credentials.CredentialsStore
import com.baulsupp.okurl.credentials.DefaultToken
import com.baulsupp.okurl.kotlin.edit
import com.baulsupp.okurl.kotlin.query
import com.baulsupp.okurl.secrets.Secrets
import com.baulsupp.okurl.util.LoggingUtil.Companion.configureLogging
import com.github.rvesse.airline.HelpOption
import com.github.rvesse.airline.SingleCommand
import com.github.rvesse.airline.annotations.Arguments
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import com.github.rvesse.airline.help.Help
import com.github.rvesse.airline.parser.errors.ParseException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.Closeable
import java.util.ArrayList
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * Simple command line tool to make a Coo.ee command.
 */
@Command(name = "cooee", description = "CLI for Coo.ee")
class Main {
  @Inject
  var help: HelpOption<Main>? = null

  @Option(name = ["-V", "--version"], description = "Show version number and quit")
  var version = false

  @Option(name = ["-l", "--local"], description = "Use local server")
  var local = false

  @Option(name = ["--option-complete"], description = "Complete options")
  var complete: String? = null

  @Option(name = ["--command-complete"], description = "Complete Command")
  var commandComplete: Boolean = false

  @Option(name = ["--debug"], description = "Debug Output")
  var debug: Boolean = false

  @Option(name = ["--login"], description = "Login")
  var login: Boolean = false

  @Arguments(title = ["arguments"], description = "Remote resource URLs")
  var arguments: MutableList<String> = ArrayList()

  lateinit var client: OkHttpClient

  lateinit var outputHandler: OutputHandler<Response>

  lateinit var credentialsStore: CredentialsStore

  val closeables = mutableListOf<Closeable>()

  private val logger = Logger.getLogger(Main::class.java.name)

  fun runCommand(runArguments: List<String>): Int {
    runBlocking {
      when {
        complete != null -> completeOption(complete!!)
        version -> printVersion()
        commandComplete -> completeCommand(arguments)
        login -> login()
        else -> cooeeCommand(arguments)
      }
    }

    return 0
  }

  private suspend fun login() {
    val user = Secrets.prompt("User", "cooee.user", System.getenv("USER"), false)
    val email = Secrets.prompt("Email", "cooee.email", "", false)
    val secret = Secrets.prompt("Secret", "cooee.secret", UUID.randomUUID().toString(), true)

    val web = Preferences.local.web ?: "https://coo.ee"

    SimpleWebServer.forCode().use { s ->
      val loginUrl = "$web/login?user=$user&email=$email&secret=$secret&callback=${s.redirectUri}"

      outputHandler.openLink(loginUrl)

      val code = s.waitForCode()

      credentialsStore.set(serviceDefinition, DefaultToken.name, Jwt(code))
    }
  }

  private fun listOptions(option: String): Collection<String> {
    return when (option) {
      "complete" -> listOf("complete")
      else -> listOf()
    }
  }

  private suspend fun completeCommand(arguments: List<String>) {
    try {
      val completionList: List<String> = buildCompletions(arguments)
      outputHandler.info(completionList.joinToString("\n"))
    } catch (ue: UsageException) {
      throw ue
    } catch (e: Exception) {
      logger.log(Level.FINE, "failure during url completion", e)
    }
  }

  private suspend fun buildCompletions(arguments: List<String>): List<String> {
    val line = arguments.getOrElse(0) { "" }
    val current = arguments.getOrNull(1)
    val editPos = arguments.getOrNull(2)?.toInt() ?: line.length

    val parts = line.split(" ")

    return if (parts.size > 1) {
      argumentCompletionQuery(line).completions
    } else {
      commandCompletionQuery(parts.getOrElse(0) { "" }).completions
    }
  }

  private fun printVersion() {
    outputHandler.info(name() + " " + versionString())
  }

  private fun name(): String {
    return "cooee"
  }

  private fun completeOption(complete: String) {
    return outputHandler.info(listOptions(complete).toSortedSet().joinToString(" "))
  }

  private fun initialise() {
    System.setProperty("apple.awt.UIElement", "true")

    if (!this::outputHandler.isInitialized) {
      outputHandler = buildHandler()
    }

    if (!this::credentialsStore.isInitialized) {
      credentialsStore = CredentialFactory.createCredentialsStore()
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

  private fun buildHandler(): OutputHandler<Response> {
    return ConsoleHandler.instance()
  }

  private fun createClientBuilder(): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()

    builder.addInterceptor {
      it.proceed(it.request().edit {
        header("User-Agent", "cooee-cli/" + versionString())

        // TODO fake for now
        header("Authorization", "Bearer " + System.getenv("USER"))
      })
    }

    return builder
  }

  private suspend fun cooeeCommand(runArguments: List<String>): Int {
    val result = bounceQuery(runArguments)

    if (result.location != null) {
      outputHandler.openLink(result.location)
      return 0
    } else {
      outputHandler.showError("No results found")
      return -1
    }
  }

  private suspend fun bounceQuery(runArguments: List<String>) =
    client.query<GoResult>("${host()}/api/v0/goinfo?q=${runArguments.joinToString(" ")}")

  private suspend fun commandCompletionQuery(query: String) =
    client.query<CompletionResult>("${host()}/api/v0/command-completion?q=$query")

  private suspend fun argumentCompletionQuery(query: String) =
    client.query<CompletionResult>("${host()}/api/v0/argument-completion?q=$query")

  private fun host() = when {
    local -> "http://localhost:8080"
    Preferences.local.api != null -> Preferences.local.api
    else -> "https://api.coo.ee"
  }

  private fun versionString(): String {
    return this.javaClass.`package`.implementationVersion ?: "dev"
  }

  suspend fun run(): Int {
    if (help?.showHelpIfRequested() == true) {
      return 0
    }

    configureLogging(debug, false, false)

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
