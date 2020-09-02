package com.baulsupp.cooee.cli

import com.baulsupp.cooee.cli.LoggingUtil.Companion.configureLogging
import com.baulsupp.oksocial.output.*
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.websocket.*
import io.ktor.util.*
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.CompositeByteBuf
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.cancel
import io.rsocket.kotlin.core.RSocketClientSupport
import io.rsocket.kotlin.core.rSocket
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.runBlocking
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.TlsVersion
import okhttp3.internal.closeQuietly
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.Closeable
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration.ofSeconds
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
  @Option(names = ["-l", "--local"], description = ["Use local server"])
  var local = false

  @Option(names = ["--option-complete"], description = ["Complete options"])
  var complete: String? = null

  @Option(names = ["--command-complete"], description = ["Complete Command"])
  var commandComplete: Boolean = false

  @Option(names = ["--debug"], description = ["Debug Output"])
  var debug: Boolean = false

  @Parameters(paramLabel = "arguments", description = ["Remote resource URLs"])
  var arguments: MutableList<String> = ArrayList()

  lateinit var client: OkHttpClient

  lateinit var outputHandler: OutputHandler<Response>

  private val closeables = mutableListOf<Closeable>()

  lateinit var rsocketClient: RSocket

  suspend fun runCommand(): Int {
    when {
      complete != null -> completeOption(complete!!)
      commandComplete -> showCompletions(arguments.last(), arguments.joinToString(" "))
      arguments.isEmpty() || arguments == listOf("") -> this@Main.showTodos()
      else -> this@Main.cooeeCommand(arguments)
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

    val token = if (tokenFile.isFile) tokenFile.readText() else null

    builder.addInterceptor {
      it.proceed(it.request().edit {
        header("User-Agent", "cooee-cli/" + versionString())

        if (token != null) {
          header("Authorization", "Bearer $token")
        }
      })
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
      }
    }

    return client.rSocket(uri, uri.startsWith("wss")).also {
      closeables.add(0) {
        it.cancel()
        client.close()
      }
    }
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
    val tokenFile = File(configDir, "token")

    val logger by lazy { Logger.getLogger("main") }

    @JvmStatic
    fun main(vararg args: String) {
      System.setProperty("io.netty.noUnsafe", "true")
      CommandLine(Main()).execute(*args)
    }
  }
}
