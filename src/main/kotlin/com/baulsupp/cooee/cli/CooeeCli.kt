package com.baulsupp.cooee.cli

import com.baulsupp.cooee.cli.LoggingUtil.Companion.configureLogging
import com.baulsupp.oksocial.output.ConsoleHandler
import com.baulsupp.oksocial.output.OutputHandler
import com.baulsupp.oksocial.output.ToStringResponseExtractor
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import kotlinx.coroutines.runBlocking
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.TlsVersion
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.Closeable
import java.io.File
import java.time.Duration.ofSeconds
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple command line tool to make a Coo.ee command.
 */
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

  @Option(names = ["--login"], description = ["Login"])
  var login: Boolean = false

  @Option(names = ["--logout"], description = ["Logout"])
  var logout: Boolean = false

  @Parameters(paramLabel = "arguments", description = ["Remote resource URLs"])
  var arguments: MutableList<String> = ArrayList()

  lateinit var client: OkHttpClient

  lateinit var outputHandler: OutputHandler<Response>

  private val closeables = mutableListOf<Closeable>()

  suspend fun runCommand(): Int {
    when {
      complete != null -> completeOption(complete!!)
      commandComplete -> completeCommand(arguments.joinToString(" "))
      login -> login()
      logout -> logout()
      arguments.isEmpty() || arguments == listOf("") -> this@Main.todoCommand()
      else -> this@Main.cooeeCommand(arguments)
    }

    return 0
  }

  private suspend fun login() {
    val web = webHost()

    SimpleWebServer.forCode().use { s ->
      outputHandler.openLink("$web/user/jwt?callback=${s.redirectUri}")

      val token = s.waitForCode()

      val jwt = parseClaims(token)
      outputHandler.info("JWT: $jwt")

      tokenFile
        .apply { parentFile.mkdirs() }
        .writeText(jwt.toString())
    }
  }

  private fun parseClaims(token: String): Claims? {
    // TODO verify using public signing key
    val unsignedToken = token.substring(0, token.lastIndexOf('.') + 1)
    return Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken).body
  }

  private fun logout() {
    tokenFile.delete()
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

  private fun initialise() {
    System.setProperty("apple.awt.UIElement", "true")

    if (!this::outputHandler.isInitialized) {
      outputHandler = ConsoleHandler.instance(ToStringResponseExtractor)
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
  }

  private fun createClientBuilder(): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()
      .callTimeout(ofSeconds(15)).connectTimeout(ofSeconds(15))
      .readTimeout(ofSeconds(15)).writeTimeout(ofSeconds(15))

    // TODO fix this to support TLSv1.3 and ECDHE ciphers
    val suites = arrayOf(
      // Note that the following cipher suites are all on HTTP/2's bad cipher suites list. We'll
      // continue to include them until better suites are commonly available.
      CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
      CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
      CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
      CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,
      CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA)

    val modernWithoutEcc =
      ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).cipherSuites(*suites)
        .tlsVersions(TlsVersion.TLS_1_2).build()
    builder.connectionSpecs(listOf(modernWithoutEcc))

//    if (debug) {
//      val loggingInterceptor = HttpLoggingInterceptor()
//      loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
//      builder.addNetworkInterceptor(loggingInterceptor)
//    }

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

  fun apiHost() = when {
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

  override fun run() {
    configureLogging(debug)

    initialise()

    runBlocking {
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
    val configDir = File(System.getProperty("user.home"), ".cooee")
    val tokenFile = File(configDir, "token")

    val logger by lazy { Logger.getLogger("main") }

    @JvmStatic
    fun main(vararg args: String) {
      CommandLine(Main()).execute(*args)
    }
  }
}
