package com.baulsupp.cooee.cli

import com.baulsupp.cooee.cli.commands.tokenResponse
import com.baulsupp.cooee.cli.auth.CooeeServiceDefinition
import com.baulsupp.cooee.cli.commands.Shell
import com.baulsupp.cooee.cli.commands.cooeeCommand
import com.baulsupp.cooee.cli.commands.showCompletions
import com.baulsupp.cooee.cli.prefs.Preferences
import com.baulsupp.cooee.cli.util.LoggingUtil.Companion.configureLogging
import com.baulsupp.cooee.cli.util.OkHttpResponseExtractor
import com.baulsupp.cooee.cli.util.edit
import com.baulsupp.cooee.cli.util.moshi
import com.baulsupp.cooee.p.LogRequest
import com.baulsupp.cooee.p.TokenRequest
import com.baulsupp.cooee.p.TokenResponse
import com.baulsupp.oksocial.output.ConsoleHandler
import com.baulsupp.oksocial.output.OutputHandler
import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.authenticator.AuthenticatingInterceptor
import com.baulsupp.okurl.authenticator.RenewingInterceptor
import com.baulsupp.okurl.credentials.DefaultToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.websocket.WebSockets
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes
import io.netty.buffer.ByteBufAllocator.DEFAULT
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.CompositeByteBuf
import io.netty.buffer.Unpooled
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.cancel
import io.rsocket.kotlin.connection.LoggingConnection
import io.rsocket.kotlin.core.RSocketClientSupport
import io.rsocket.kotlin.core.rSocket
import io.rsocket.kotlin.error.RSocketError
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.metadata.AuthMetadataCodec
import io.rsocket.metadata.CompositeMetadata
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadata
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.LoggingEventListener
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Help
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Simple command line tool to make a Coo.ee command.
 */
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

  @Option(names = ["--local", "-l"], description = ["Local Server"])
  var local: Boolean = false

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

    rsocketClient = buildClient(if (local) "ws://localhost:8080/rsocket" else Preferences.local.api)
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
      install(RSocketClientSupport) {
        payloadMimeType = PayloadMimeType("application/json",
          "message/x.rsocket.composite-metadata.v0")

        this.setupPayload = setupPayload

        acceptor = {
          RSocketRequestHandler {
            fireAndForget = {
              val route = it.metadata?.let { parseRoute(it) }

              if (route == "log") {
                val request = moshi.adapter(LogRequest::class.java).fromJson(it.data.readText())
                System.err.println("Error: ${
                  Help.Ansi.AUTO.string(
                    " @|yellow [${request?.severity}] ${request?.message}|@")
                }")
              } else {
                throw RSocketError.ApplicationError("Unknown route: $route")
              }
            }
            requestResponse = {
              val route = it.metadata?.let { parseRoute(it) }

              if (route == "token") {
                // TokenRequest
                val request = moshi.adapter(TokenRequest::class.java).fromJson(it.data.readText())!!
                val response = tokenResponse(request)
                Payload(moshi.adapter(TokenResponse::class.java).toJson(response))
              } else {
                throw RSocketError.ApplicationError("Unknown route: $route")
              }
            }
          }
        }
        keepAlive = KeepAlive(5.seconds)
        if (debug) {
          plugin = plugin.copy(connection = listOf(::LoggingConnection))
        }
      }
    }

    return client.rSocket(uri, secure = uri.startsWith("wss")).also {
      closeables.add(0) {
        it.cancel()
        client.close()
      }
    }
  }

  suspend fun buildSetupPayload(): Payload? {
    val token = credentialsStore.get(CooeeServiceDefinition, DefaultToken.name)

    if (token != null) {
      val routingMetadata = AuthMetadataCodec.encodeBearerMetadata(DEFAULT, token.toCharArray())
      val compositeByteBuf = CompositeByteBuf(DEFAULT, false, 1)
      CompositeMetadataCodec.encodeAndAddMetadata(compositeByteBuf, DEFAULT,
        WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION, routingMetadata)
      val metadataBytes = ByteBufUtil.getBytes(compositeByteBuf)

      return Payload(byteArrayOf(), metadataBytes)
    }

    return null
  }

  fun parseRoute(metadata: ByteReadPacket): String? {
    CompositeMetadata(Unpooled.wrappedBuffer(metadata.readBytes()), false).forEach {
      if (it.mimeType == "message/x.rsocket.routing.v0") {
        TaggingMetadata("message/x.rsocket.routing.v0", it.content).forEach { route ->
          return route
        }
      }
    }

    return null
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
      System.exit(-1)
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

    val logger by lazy { Logger.getLogger("main") }

    @JvmStatic
    fun main(vararg args: String) {
      System.setProperty("io.netty.noUnsafe", "true")
      CommandLine(Main()).execute(*args)
    }
  }
}
