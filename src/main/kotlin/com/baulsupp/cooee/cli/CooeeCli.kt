package com.baulsupp.cooee.cli

import com.baulsupp.cooee.cli.repl.Repl
import com.baulsupp.oksocial.output.ConsoleHandler
import com.baulsupp.oksocial.output.OutputHandler
import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.authenticator.AuthenticatingInterceptor
import com.baulsupp.okurl.authenticator.Jwt
import com.baulsupp.okurl.authenticator.SimpleWebServer
import com.baulsupp.okurl.commands.ToolSession
import com.baulsupp.okurl.credentials.CredentialFactory
import com.baulsupp.okurl.credentials.CredentialsStore
import com.baulsupp.okurl.credentials.DefaultToken
import com.baulsupp.okurl.credentials.TokenSet
import com.baulsupp.okurl.kotlin.edit
import com.baulsupp.okurl.kotlin.execute
import com.baulsupp.okurl.kotlin.query
import com.baulsupp.okurl.kotlin.request
import com.baulsupp.okurl.location.BestLocation
import com.baulsupp.okurl.location.LocationSource
import com.baulsupp.okurl.okhttp.OkHttpResponseExtractor
import com.baulsupp.okurl.services.ServiceLibrary
import com.baulsupp.okurl.services.cooee.CooeeAuthInterceptor
import com.baulsupp.okurl.util.ClientException
import com.baulsupp.okurl.util.LoggingUtil.Companion.configureLogging
import com.github.markusbernhardt.proxy.ProxySearch
import com.github.rvesse.airline.HelpOption
import com.github.rvesse.airline.SingleCommand
import com.github.rvesse.airline.annotations.Arguments
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import com.github.rvesse.airline.help.Help
import com.github.rvesse.airline.parser.errors.ParseException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import kotlinx.coroutines.CoroutineStart.ATOMIC
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.Closeable
import java.time.Duration.*
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.system.exitProcess

val emptyBody = RequestBody.create(MediaType.get("text/plain"), "")

/**
 * Simple command line tool to make a Coo.ee command.
 */
@Command(name = "cooee", description = "CLI for Coo.ee")
class Main : ToolSession {
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

  @Option(name = ["--fish-complete"], description = "Complete Command")
  var fishComplete: Boolean = false

  @Option(name = ["--debug"], description = "Debug Output")
  var debug: Boolean = false

  @Option(name = ["--login"], description = "Login")
  var login: Boolean = false

  @Option(name = ["--logout"], description = "Logout")
  var logout: Boolean = false

  @Option(name = ["--authorize"], description = "Authorize Service")
  var authorize: String? = null

  @Option(name = ["--token"], description = "Auth Token")
  var token: String? = null

  @Option(name = ["--tokenSet"], description = "Token Set")
  override var defaultTokenSet: TokenSet = DefaultToken

  @Option(name = ["--add"], description = "Add Provider")
  var addProvider: String? = null

  @Option(name = ["--remove"], description = "Add Provider")
  var removeProvider: String? = null

  @Option(name = ["--list"], description = "Add Provider")
  var listProvider = false

  @Option(name = ["--repl"], description = "Repl")
  var repl = false

  @Arguments(title = ["arguments"], description = "Remote resource URLs")
  var arguments: MutableList<String> = ArrayList()

  override lateinit var client: OkHttpClient

  override lateinit var outputHandler: OutputHandler<Response>

  override lateinit var credentialsStore: CredentialsStore

  override lateinit var locationSource: LocationSource

  override lateinit var serviceLibrary: ServiceLibrary

  val closeables = mutableListOf<Closeable>()

  private val logger = Logger.getLogger(Main::class.java.name)

  private val serviceDefinition = CooeeAuthInterceptor().serviceDefinition

  fun runCommand(): Int {
    runBlocking {
      when {
        complete != null -> completeOption(complete!!)
        version -> printVersion()
        commandComplete -> ShellCompletion(
          this@Main,
          apiHost(),
          Shell.BASH
        ).completeCommand(arguments.joinToString(" "))
        fishComplete -> ShellCompletion(this@Main, apiHost(), Shell.FISH).completeCommand(arguments.joinToString(" "))
        login -> login()
        logout -> logout()
        authorize != null -> authorize()
        addProvider != null -> addProvider()
        removeProvider != null -> removeProvider()
        listProvider -> listProviders()
        repl -> launchRepl()
        arguments.isEmpty() || arguments == listOf("") -> {}
        else -> cooeeCommand(arguments)
      }
    }

    return 0
  }

  private fun launchRepl() {
    val repl = Repl(this, apiHost())
    repl.run()
  }

  private suspend fun addProvider() {
    ProviderTools(client).add(this.addProvider!!, ProviderRequest(mapOf()))
  }

  private suspend fun removeProvider() {
    ProviderTools(client).remove(this.removeProvider!!)
  }

  private suspend fun listProviders() {
    ProviderTools(client).list().forEach {
      outputHandler.info(it.name + "\t" + (if (it.installed) "installed" else "not installed"))
    }
  }

  private suspend fun authorize() {
    val token = cooeeToken()?.token
    if (authorize == "strava") {
      outputHandler.openLink("${apiHost()}/web/authenticate/$authorize?token=$token")
    } else {
      ServiceAuthorisation(this, apiHost()).authorize(authorize!!, token, defaultTokenSet)
    }
  }

  private suspend fun login() {
    val web = webHost()

    SimpleWebServer.forCode().use { s ->
      outputHandler.openLink("$web/user/jwt?callback=${s.redirectUri}")

      val token = s.waitForCode()

      val jwt = parseClaims(token)
      outputHandler.info("JWT: $jwt")

      credentialsStore.set(serviceDefinition, DefaultToken.name, Jwt(token))
    }
  }

  private fun parseClaims(token: String): Claims? {
    // TODO verify using public signing key
    val unsignedToken = token.substring(0, token.lastIndexOf('.') + 1)
    return Jwts.parser().parseClaimsJwt(unsignedToken).body
  }

  private suspend fun logout() {
    credentialsStore.remove(serviceDefinition, DefaultToken.name)
  }

  private suspend fun listOptions(option: String): Collection<String> {
    return when (option) {
      "option-complete" -> listOf("command-complete", "authorize", "add", "remove", "option-complete")
      "authorize" -> ProviderTools(client).list().flatMap { it.services }
      "add" -> ProviderTools(client).list().filter { !it.installed }.map { it.name }
      "remove" -> ProviderTools(client).list().filter { it.installed }.map { it.name }
      else -> listOf()
    }
  }

  private fun printVersion() {
    outputHandler.info(name() + " " + versionString())
  }

  private fun name(): String {
    return "cooee"
  }

  private suspend fun completeOption(complete: String) {
    return outputHandler.info(listOptions(complete).toSortedSet().joinToString("\n"))
  }

  fun initialise() {
    System.setProperty("apple.awt.UIElement", "true")

    if (!this::outputHandler.isInitialized) {
      outputHandler = buildHandler()
    }

    if (!this::credentialsStore.isInitialized) {
      credentialsStore = CredentialFactory.createCredentialsStore()
    }

    if (!this::locationSource.isInitialized) {
      locationSource = BestLocation(outputHandler)
    }

    if (!this::serviceLibrary.isInitialized) {
      serviceLibrary = OkurlServiceLibrary
    }

    if (!this::authenticatingInterceptor.isInitialized) {
      authenticatingInterceptor = AuthenticatingInterceptor(this.credentialsStore)
    }

    closeables.add(Closeable {
      if (this::client.isInitialized) {
        client.connectionPool().evictAll()
        client.dispatcher().executorService().shutdownNow()
      }
    })

    if (!this::client.isInitialized) {
      val clientBuilder = createClientBuilder()

      client = clientBuilder.build()
    }
  }

  private fun buildHandler(): OutputHandler<Response> {
    return ConsoleHandler.instance(OkHttpResponseExtractor())
  }

  private fun applyProxy(builder: OkHttpClient.Builder) {
    when {
      Preferences.local.proxy != null -> builder.proxy(Preferences.local.proxy!!.build())
      else -> builder.proxySelector(ProxySearch.getDefaultProxySearch().proxySelector)
    }
  }

  private lateinit var authenticatingInterceptor: AuthenticatingInterceptor

  private fun createClientBuilder(): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()
      .callTimeout(ofSeconds(15)).connectTimeout(ofSeconds(15))
      .readTimeout(ofSeconds(15)).writeTimeout(ofSeconds(15))

    if (debug) {
      val loggingInterceptor = HttpLoggingInterceptor()
      loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
      builder.addNetworkInterceptor(loggingInterceptor)
    }

    applyProxy(builder)

    builder.addInterceptor {
      it.proceed(it.request().edit {
        header("User-Agent", "cooee-cli/" + versionString())

        if (local && it.request().url().host() == "localhost") {
          val token = runBlocking { cooeeToken() }

          if (token != null) {
            header("Authorization", "Bearer ${token.token}")
          }
        }
      })
    }

    builder.addNetworkInterceptor(authenticatingInterceptor)

    return builder
  }

  private suspend fun cooeeToken() = credentialsStore.get(serviceDefinition, DefaultToken)

  suspend fun cooeeCommand(runArguments: List<String>): Int = coroutineScope {
    try {
      val result = bounceQuery(runArguments)

      if (result.location != null || result.message != null || result.image != null) {
        var imageResponse: Deferred<Response>? = null
        if (result.image != null) {
          imageResponse = async { client.execute(request(result.image)) }
        }
        if (result.message != null) {
          outputHandler.info(result.message)
        }
        if (result.location != null) {
          @Suppress("EXPERIMENTAL_API_USAGE")
          launch(start = ATOMIC) {
            outputHandler.openLink(result.location)
          }
        }
        if (imageResponse != null) {
          outputHandler.showOutput(imageResponse.await())
        }
        0
      } else {
        outputHandler.showError("No results found")
        -1
      }
    } catch (ce: ClientException) {
      val message = ce.responseMessage
      outputHandler.showError(message = message)

      -2
    }
  }

  private suspend fun bounceQuery(runArguments: List<String>) =
    client.query<GoResult>("${apiHost()}/api/v0/goinfo?q=${runArguments.joinToString(" ")}")

  private fun apiHost() = when {
    local -> "http://localhost:8080"
    else -> Preferences.local.api
  }

  private fun webHost() = when {
    local -> "http://localhost:5000"
    else -> Preferences.local.web
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
      runCommand()
      0
    } catch (e: UsageException) {
      outputHandler.showError(e.message)
      -1
    } catch (e: ClientException) {
      outputHandler.showError(e.message)
      -2
    } catch (e: Exception) {
      outputHandler.showError("unknown error", e)
      -3
    } finally {
      close()
    }
  }

  override fun close() {
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

@ExperimentalCoroutinesApi
suspend fun main(vararg args: String) {
//  DebugProbes.install()

  fromArgs(*args).run()

  exitProcess(-1)
}
