package com.baulsupp.cooee.sdk

import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.cli.util.LoggingUtil
import com.baulsupp.schoutput.UsageException
import kotlinx.coroutines.runBlocking
import java.io.IOException

class SimpleClient {
  val main = Main()

  suspend fun run(vararg command: String) {
    main.arguments = command.toMutableList()

    System.setProperty("io.netty.noUnsafe", "true")
    LoggingUtil.configureLogging(false)

    runBlocking {
      try {
        main.initialise()

        main.runCommand()
      } catch (ue: UsageException) {
        main.outputHandler.showError(ue.message)
      } catch (ioe: IOException) {
        // ignore
        main.outputHandler.showError(ioe.message)
      } finally {
        main.close()
      }
    }
  }
}
