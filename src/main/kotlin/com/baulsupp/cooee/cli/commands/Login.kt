package com.baulsupp.cooee.cli.commands

import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.p.TokenRequest
import com.baulsupp.cooee.p.TokenResponse
import com.baulsupp.cooee.p.TokenUpdate
import com.baulsupp.okurl.authenticator.AuthInterceptor
import com.baulsupp.okurl.credentials.DefaultToken
import com.baulsupp.okurl.credentials.TokenSet
import java.lang.IllegalStateException

suspend fun Main.tokenResponse(request: TokenRequest): TokenResponse? {
  suspend fun <T> AuthInterceptor<T>.getTokenString(
    request: TokenRequest
  ): String? {
    val tokenSet = request.token_set?.let { TokenSet(it) } ?: DefaultToken
    val token = credentialsStore.get(serviceDefinition, tokenSet) ?: return null
    return serviceDefinition.formatCredentialsString(token)
  }

  return when {
    request.login_url != null -> {
      outputHandler.showError("Authenticating externally")

      outputHandler.openLink(request.login_url)

      TokenResponse(login_attempted = true)
    }
    request.token != null -> {
      val serviceName = request.service
      val service = services.find { it.name() == serviceName } ?: throw IllegalStateException(
        "unknown service $serviceName")
      updateToken(service, DefaultToken.name, request.token)
      TokenResponse()
    }
    else -> {
      val serviceName = request.service
      val service = services.find { it.name() == serviceName }
      val tokenString = service?.getTokenString(request)

      TokenResponse(
        token = tokenString?.let { TokenUpdate(service = serviceName, token = tokenString) })
    }
  }
}

suspend fun <T> Main.updateToken(service: AuthInterceptor<T>, tokenSet: String, tokenString: String) {
  // TODO skip parsing and store the string
  outputHandler.info("Updating ${service.serviceDefinition.shortName()} token")
  val token = service.serviceDefinition.parseCredentialsString(tokenString)
  credentialsStore.set(serviceDefinition = service.serviceDefinition, tokenSet = tokenSet, credentials = token)
}
