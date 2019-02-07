@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.baulsupp.cooee.cli

import kotlinx.coroutines.runBlocking
import org.junit.Test

class ApplicationTest {
  @Test
  fun x() = runBlocking {
    main("--remove", "trello")
  }
}
