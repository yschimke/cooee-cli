package com.baulsupp.cooee.cli

import com.baulsupp.cooee.cli.auth.CooeeServiceDefinition
import com.baulsupp.cooee.cli.commands.Shell
import com.baulsupp.cooee.cli.commands.cooeeCommand
import com.baulsupp.cooee.cli.commands.showCompletions
import com.baulsupp.cooee.cli.commands.tokenResponse
import com.baulsupp.cooee.cli.prefs.Preferences
import com.baulsupp.cooee.cli.util.LoggingUtil.Companion.configureLogging
import com.baulsupp.cooee.cli.util.OkHttpResponseExtractor
import com.baulsupp.cooee.cli.util.edit
import com.baulsupp.cooee.cli.util.moshi
import com.baulsupp.cooee.p.LogRequest
import com.baulsupp.cooee.p.TokenRequest
import com.baulsupp.cooee.p.TokenResponse
import com.baulsupp.okurl.authenticator.AuthenticatingInterceptor
import com.baulsupp.okurl.authenticator.RenewingInterceptor
import com.baulsupp.okurl.credentials.DefaultToken
import com.baulsupp.okurl.services.ServiceList
import com.baulsupp.schoutput.UsageException
import com.baulsupp.schoutput.handler.OutputHandler
import com.baulsupp.schoutput.outputHandlerInstance
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.util.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.ExperimentalMetadataApi
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketError
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.ktor.client.RSocketSupport
import io.rsocket.kotlin.ktor.client.rSocket
import io.rsocket.kotlin.logging.JavaLogger
import io.rsocket.kotlin.logging.NoopLogger
import io.rsocket.kotlin.metadata.*
import io.rsocket.kotlin.metadata.security.BearerAuthMetadata
import io.rsocket.kotlin.payload.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.LoggingEventListener
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Simple command line tool to make a Coo.ee command.
 */
@Command(name = "cooee", description = ["CLI for Coo.ee"],
  mixinStandardHelpOptions = true, version = ["dev"])
@OptIn(ExperimentalMetadataApi::class)
class Main : Runnable {
  @Option(names = ["--option-complete"], description = ["Complete options"])
  var complete: String? = null

  @Option(names = ["--command-complete"], description = ["Complete command"])
  var commandComplete: Boolean = false

  @Option(names = ["--debug"], description = ["Debug Output"])
  var debug: Boolean = false

  @Option(names = ["--open", "-o"], description = ["Open External Links"])
  var open: Boolean = false

  @Option(names = ["--local", "-l"], description = ["Local Server"])
  var local: Boolean = false

  @Parameters(paramLabel = "arguments", description = ["Remote resource URLs"])
  var arguments: MutableList<String> = ArrayList()

  lateinit var client: OkHttpClient

  lateinit var outputHandler: OutputHandler<Response>

  private val closeables = mutableListOf<suspend () -> Unit>()

  lateinit var rsocketClient: RSocket

  val services = ServiceList.defaultServices()

  val credentialsStore = com.baulsupp.okurl.credentials.SimpleCredentialsStore.also {
    File(System.getProperty("user.home"), ".okurl").mkdirs()
  }

  suspend fun runCommand(): Int {
    when {
      complete != null -> completeOption(complete!!)
      commandComplete -> showCompletions(arguments.joinToString(" "), Shell.ZSH)
      else -> this@Main.cooeeCommand(open, arguments)
    }

    return 0
  }

  private fun listOptions(option: String): Collection<String> {
    return when (option) {
      "option-complete" -> listOf("command-complete", "option-complete")
      else -> listOf()
    }
  }

  private fun completeOption(complete: String) {
    return outputHandler.info(listOptions(complete).toSortedSet().joinToString("\n"))
  }

  suspend fun initialise() {
    System.setProperty("apple.awt.UIElement", "true")

    if (!this::outputHandler.isInitialized) {
      outputHandler = outputHandlerInstance(OkHttpResponseExtractor)
    }

    if (!this::client.isInitialized) {
      val clientBuilder = createClientBuilder()

      client = clientBuilder.build()
    }

    rsocketClient = buildClient(if (local) "ws://localhost:8080/rsocket" else Preferences.local.api)

    closeables.add {
      rsocketClient.cancel()
    }

    closeables.add {
      client.connectionPool.evictAll()
      client.dispatcher.executorService.shutdownNow()
    }
  }

  private fun createClientBuilder(): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()

    builder.cache(Cache(cacheDir, 50 * 1024 * 1024))

    builder.addInterceptor(RenewingInterceptor(credentialsStore))
    builder.addInterceptor(BrotliInterceptor)

    val authenticatingInterceptor = AuthenticatingInterceptor(credentialsStore)
    builder.addNetworkInterceptor(authenticatingInterceptor)

    builder.addInterceptor {
      it.proceed(it.request().edit {
        header("User-Agent", "cooee-cli/" + versionString())
      })
    }

    if (debug) {
      builder.eventListenerFactory(LoggingEventListener.Factory())
    }

    return builder
  }

  @OptIn(ExperimentalTime::class, KtorExperimentalAPI::class)
  suspend fun buildClient(uri: String): RSocket {
    val setupPayload = buildSetupPayload() ?: Payload.Empty

    val client = HttpClient(OkHttp) {
      engine {
        preconfigured = client
      }

      install(WebSockets)
      install(RSocketSupport) {
        connector = RSocketConnector {
          loggerFactory = if (debug) JavaLogger else NoopLogger

          connectionConfig {
            setupPayload { setupPayload }
            keepAlive = KeepAlive(5.seconds)
            payloadMimeType = PayloadMimeType("application/json",
              "message/x.rsocket.composite-metadata.v0")
          }
          acceptor {
            RSocketRequestHandler {
              fireAndForget {
                val route = it.metadata?.let { route -> parseRoute(route) }

                if (route == "log") {
                  @Suppress("BlockingMethodInNonBlockingContext")
                  val request = moshi.adapter(LogRequest::class.java).fromJson(it.data.readText())
                  System.err.println("Error: ${
                    Help.Ansi.AUTO.string(
                      " @|yellow [${request?.severity}] ${request?.message}|@")
                  }")
                } else {
                  throw RSocketError.ApplicationError("Unknown route: $route")
                }
              }
              requestResponse { rsocket ->
                val route = rsocket.metadata?.let { route -> parseRoute(route) }

                if (route == "token") {
                  // TokenRequest
                  @Suppress("BlockingMethodInNonBlockingContext",
                    "BlockingMethodInNonBlockingContext"
                  )
                  val request = moshi.adapter(TokenRequest::class.java).fromJson(rsocket.data.readText())!!
                  val response = tokenResponse(request)
                  buildPayload {
                    data(moshi.adapter(TokenResponse::class.java).toJson(response))
                  }
                } else {
                  throw RSocketError.ApplicationError("Unknown route: $route")
                }
              }
            }
          }
        }
      }
    }

    return client.rSocket(uri, secure = uri.startsWith("wss")).also { rsocket ->
      closeables.add(0) {
        rsocket.cancel()
        client.close()
      }
    }
  }

  private suspend fun buildSetupPayload(): Payload? {
    val token = credentialsStore.get(CooeeServiceDefinition, DefaultToken.name)

    if (token != null) {
      return buildPayload {
        compositeMetadata {
          add(BearerAuthMetadata(token))
        }
      }
    }

    return null
  }

  private fun parseRoute(metadata: ByteReadPacket): String? {
    return metadata.read(CompositeMetadata).getOrNull(RoutingMetadata)?.tags?.firstOrNull()
  }

  private fun versionString(): String {
    return this.javaClass.`package`.implementationVersion ?: "dev"
  }

  override fun run() {
    System.setProperty("io.netty.noUnsafe", "true")
    configureLogging(debug)

    runBlocking {
      try {
        initialise()

        runCommand()
      } catch (ue: UsageException) {
        outputHandler.showError(ue.message)
      } catch (ioe: IOException) {
        // ignore
        outputHandler.showError(ioe.message)
      } finally {
        close()
      }
      exitProcess(-1)
    }
  }

  suspend fun close() {
    for (c in closeables) {
      try {
        c()
      } catch (e: Exception) {
        logger.log(Level.WARNING, "close failed", e)
      }
    }
  }

  companion object {
    val configDir = File(System.getProperty("user.home"), ".cooee").also {
      it.mkdirs()
    }
    val cacheDir = File(configDir, "cache")

    val logger: Logger by lazy { Logger.getLogger("main") }

    @JvmStatic
    fun main(vararg args: String) {
      System.setProperty("io.netty.noUnsafe", "true")
      CommandLine(Main()).execute(*args)
    }
  }
}
