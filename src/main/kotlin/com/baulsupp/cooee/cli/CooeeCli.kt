package com.baulsupp.cooee.cli

import com.baulsupp.cooee.cli.LoggingUtil.Companion.configureLogging
import com.baulsupp.cooee.p.LogRequest
import com.baulsupp.cooee.p.TokenRequest
import com.baulsupp.cooee.p.TokenResponse
import com.baulsupp.oksocial.output.*
import com.baulsupp.okurl.authenticator.AuthInterceptor
import com.baulsupp.okurl.authenticator.AuthenticatingInterceptor
import com.baulsupp.okurl.authenticator.RenewingInterceptor
import com.baulsupp.okurl.credentials.DefaultToken
import com.baulsupp.okurl.credentials.TokenSet
import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.websocket.*
import io.ktor.util.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.cancel
import io.rsocket.kotlin.core.RSocketClientSupport
import io.rsocket.kotlin.core.rSocket
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.PayloadMimeType
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.LoggingEventListener
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.Closeable
import java.io.File
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple command line tool to make a Coo.ee command.
 */
@KtorExperimentalAPI
@Command(name = "cooee", description = ["CLI for Coo.ee"],
  mixinStandardHelpOptions = true, version = ["dev"])
class Main : Runnable {
  @Option(names = ["--option-complete"], description = ["Complete options"])
  var complete: String? = null

  @Option(names = ["--command-complete"], description = ["Complete command"])
  var commandComplete: Boolean = false

  @Option(names = ["--debug"], description = ["Debug Output"])
  var debug: Boolean = false

  @Option(names = ["--open"], description = ["Open External Links"])
  var open: Boolean = false

  @Parameters(paramLabel = "arguments", description = ["Remote resource URLs"])
  var arguments: MutableList<String> = ArrayList()

  lateinit var client: OkHttpClient

  lateinit var outputHandler: OutputHandler<Response>

  private val closeables = mutableListOf<Closeable>()

  lateinit var rsocketClient: RSocket

  val services = AuthenticatingInterceptor.defaultServices()

  val credentialsStore = com.baulsupp.okurl.credentials.SimpleCredentialsStore.also {
    File(System.getProperty("user.home"), ".okurl").mkdirs()
  }

  suspend fun runCommand(): Int {
    when {
      complete != null -> completeOption(complete!!)
      commandComplete -> showCompletions(arguments.last(), arguments.joinToString(" "), Shell.ZSH)
      arguments.isEmpty() || arguments == listOf("") -> this@Main.showTodos()
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
      outputHandler = ConsoleHandler.instance(OkHttpResponseExtractor)
    }

    closeables.add(Closeable {
      if (this::client.isInitialized) {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdownNow()
      }
    })

    if (!this::client.isInitialized) {
      val clientBuilder = createClientBuilder()

      client = clientBuilder.build()
    }

    rsocketClient = buildClient("ws://localhost:8080/rsocket")
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

  suspend fun buildClient(uri: String): RSocket {
    val client = HttpClient(OkHttp) {
      engine {
        preconfigured = client
      }

      install(WebSockets)
      install(RSocketClientSupport) {
        payloadMimeType = PayloadMimeType("application/json", "message/x.rsocket.composite-metadata.v0")
        acceptor = {
          RSocketRequestHandler {
            fireAndForget = {
              val request = moshi.adapter(LogRequest::class.java).fromJson(it.data.readText())
              System.err.println("Error: ${Help.Ansi.AUTO.string(" @|yellow [${request?.severity}] ${request?.message}|@")}")
            }
            requestResponse = {
              // TokenRequest
              val request = moshi.adapter(TokenRequest::class.java).fromJson(it.data.readText())
              val response = tokenResponse(request)
              Payload(moshi.adapter<TokenResponse>(TokenResponse::class.java).toJson(response))
            }
          }
        }
      }
    }

    return client.rSocket(uri, uri.startsWith("wss")).also {
      closeables.add(0) {
        it.cancel()
        client.close()
      }
    }
  }

  suspend fun tokenResponse(request: TokenRequest?): TokenResponse? {
    val serviceName = request!!.service ?: return null

    val service = services.find { it.name() == serviceName }!!
    val tokenString = service.getTokenString(serviceName, request)

    return TokenResponse(token = tokenString)
  }

  suspend fun <T> AuthInterceptor<T>.getTokenString(serviceName: String, request: TokenRequest): String? {
    val tokenSet = request.name?.let { TokenSet(it) } ?: DefaultToken
    val token = credentialsStore.get(serviceDefinition, tokenSet) ?: return null
    return serviceDefinition.formatCredentialsString(token)
  }

  private fun versionString(): String {
    return this.javaClass.`package`.implementationVersion ?: "dev"
  }

  override fun run() {
    System.setProperty("io.netty.noUnsafe", "true")
    configureLogging(debug)

    runBlocking {
      initialise()

      try {
        runCommand()
      } catch (ue: UsageException) {
        outputHandler.showError(ue.message)
      } finally {
        close()
      }
    }
  }

  fun close() {
    for (c in closeables) {
      try {
        c.close()
      } catch (e: Exception) {
        logger.log(Level.FINE, "close failed", e)
      }
    }
  }

  companion object {
    val configDir = File(System.getProperty("user.home"), ".cooee").also {
      it.mkdirs()
    }
    val cacheDir = File(configDir, "cache")
    val tokenFile = File(configDir, "token")

    val logger by lazy { Logger.getLogger("main") }

    @JvmStatic
    fun main(vararg args: String) {
      System.setProperty("io.netty.noUnsafe", "true")
      CommandLine(Main()).execute(*args)
    }
  }
}
