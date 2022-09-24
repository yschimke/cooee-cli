package com.baulsupp.cooee.sdk

import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.cli.commands.requestResponse
import com.baulsupp.cooee.cli.prefs.Preferences
import com.baulsupp.cooee.cli.util.LoggingUtil
import com.baulsupp.cooee.cli.util.edit
import com.baulsupp.cooee.cli.util.moshi
import com.baulsupp.cooee.p.CommandRequest
import com.baulsupp.cooee.p.CommandResponse
import com.baulsupp.cooee.p.CompletionRequest
import com.baulsupp.cooee.p.CompletionResponse
import com.baulsupp.cooee.p.CompletionSuggestion
import com.baulsupp.cooee.p.LogRequest
import com.baulsupp.cooee.p.RegisterServerRequest
import com.baulsupp.cooee.p.RegisterServerResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.utils.io.core.ByteReadPacket
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
import io.rsocket.kotlin.metadata.CompositeMetadata
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.getOrNull
import io.rsocket.kotlin.metadata.read
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.LoggingEventListener
import picocli.CommandLine
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(ExperimentalMetadataApi::class)
class SimpleServer(val debug: Boolean = false, val local: Boolean = false) {
  lateinit var command: String

  lateinit var commandHandler: suspend (CommandRequest) -> Flow<CommandResponse>

  lateinit var completionHandler: suspend (CompletionRequest) -> CompletionResponse

  private lateinit var rsocketClient: RSocket

  private lateinit var client: OkHttpClient

  private val closeables = mutableListOf<suspend () -> Unit>()

  fun exportSingleCommand(command: String, fn: suspend (CommandRequest) -> Flow<CommandResponse>) {
    this.command = command
    this.completionHandler = {
      CompletionResponse(listOf(CompletionSuggestion(word = command)))
    }
    commandHandler = fn

    runBlocking {
      start()
    }
  }

  fun exportSimpleCommands(command: String, completer: suspend (CompletionRequest) -> List<String>, fn: suspend (CommandRequest) -> Flow<CommandResponse>) {
    this.command = command
    this.completionHandler = { request ->
      CompletionResponse(completions = completer(request).map { CompletionSuggestion(word = it) })
    }
    commandHandler = fn

    runBlocking {
      start()
    }
  }

  suspend fun start() {
    System.setProperty("apple.awt.UIElement", "true")
    LoggingUtil.configureLogging(debug)

    if (!this::client.isInitialized) {
      val clientBuilder = createClientBuilder()

      client = clientBuilder.build()
    }

    rsocketClient = buildClient(if (local) "ws://localhost:8080/rsocket" else Preferences.local.api)

    val response = rsocketClient.requestResponse<RegisterServerRequest, RegisterServerResponse>(
      "registerServer", RegisterServerRequest(commands = listOf(command))
    )

    println("Registered $response")
  }

  private fun createClientBuilder(): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()

    builder.cache(Cache(Main.cacheDir, 50 * 1024 * 1024))

    // builder.addInterceptor(RenewingInterceptor(credentialsStore))
    builder.addInterceptor(BrotliInterceptor)

    // val authenticatingInterceptor = AuthenticatingInterceptor(credentialsStore)
    // builder.addNetworkInterceptor(authenticatingInterceptor)

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

  private fun versionString(): String {
    return this.javaClass.`package`.implementationVersion ?: "dev"
  }

  @OptIn(ExperimentalTime::class)
  suspend fun buildClient(uri: String): RSocket {
    val client = HttpClient(OkHttp) {
      engine {
        preconfigured = client
      }

      WebSockets {  }
      install(RSocketSupport) {
        connector = RSocketConnector {
          loggerFactory = if (debug) JavaLogger else NoopLogger

          connectionConfig {
            setupPayload { buildSetupPayload() }
            keepAlive = KeepAlive(5.seconds)
            payloadMimeType = PayloadMimeType(
              "application/json",
              "message/x.rsocket.composite-metadata.v0"
            )
          }
          acceptor {
            RSocketRequestHandler {
              fireAndForget { requestPayload ->
                when (val route = requestPayload.metadata?.let { route -> parseRoute(route) }) {
                  "log" -> {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val request =
                      moshi.adapter(LogRequest::class.java).fromJson(requestPayload.data.readText())
                    System.err.println(
                      CommandLine.Help.Ansi.AUTO.string(
                        " @|yellow [${request?.severity}] ${request?.message}|@"
                      )
                    )
                  }
                  else -> {
                    throw RSocketError.ApplicationError("Unknown route: $route")
                  }
                }
              }
              requestResponse { requestPayload ->
                when (val route = requestPayload.metadata?.let { route -> parseRoute(route) }) {
                  "complete" -> {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val request = moshi.adapter(CompletionRequest::class.java)
                      .fromJson(requestPayload.data.readText())!!
                    val responseAdapter = moshi.adapter(CompletionResponse::class.java)
                    val response = complete(request)
                    buildPayload {
                      data(responseAdapter.toJson(response))
                    }
                  }
                  else -> {
                    throw RSocketError.ApplicationError("Unknown route: $route")
                  }
                }
              }
              requestStream { requestPayload ->
                when (val route = requestPayload.metadata?.let { route -> parseRoute(route) }) {
                  "runCommand" -> {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val request = moshi.adapter(CommandRequest::class.java)
                      .fromJson(requestPayload.data.readText())!!
                    val responseAdapter = moshi.adapter(CommandResponse::class.java)
                    flow {
                      emitAll(runCommand(request).map {
                        buildPayload {
                          data(responseAdapter.toJson(it))
                        }
                      })
                    }
                  }
                  else -> {
                    throw RSocketError.ApplicationError("Unknown route: $route")
                  }
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

  private suspend fun runCommand(request: CommandRequest): Flow<CommandResponse> {
    return commandHandler(request)
  }

  private suspend fun complete(request: CompletionRequest): CompletionResponse {
    return completionHandler(request)
  }

  private fun buildSetupPayload(): Payload {
    return buildPayload {
      data(byteArrayOf())
      // compositeMetadata {
      //
      // }
    }
  }

  private fun parseRoute(metadata: ByteReadPacket): String? {
    return metadata.read(CompositeMetadata).getOrNull(RoutingMetadata)?.tags?.firstOrNull()
  }
}
