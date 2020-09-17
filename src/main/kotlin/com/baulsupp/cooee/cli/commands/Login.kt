package com.baulsupp.cooee.cli.commands

import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.cli.SimpleWebServer
import com.baulsupp.cooee.cli.auth.CooeeServiceDefinition
import com.baulsupp.okurl.credentials.DefaultToken
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun Main.login() {
  coroutineScope {
    launch(start = CoroutineStart.ATOMIC) {
      SimpleWebServer.forCode().use { s ->
        outputHandler.openLink(
          "https://www.coo.ee/user/jwt?callback=http://localhost:3000/callback")

        val token = s.waitForCode()

        val jwt = parseClaims(token)
        outputHandler.info("JWT: $jwt")

        credentialsStore.set(CooeeServiceDefinition, DefaultToken.name, token)
      }
    }
  }
}

private fun parseClaims(token: String): Claims? {
  // TODO verify using public signing key
  val unsignedToken = token.substring(0, token.lastIndexOf('.') + 1)
  return Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken).body
}
