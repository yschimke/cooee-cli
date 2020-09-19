package a

import com.baulsupp.cooee.cli.Main
import com.baulsupp.cooee.p.TokenRequest
import com.baulsupp.cooee.p.TokenResponse
import com.baulsupp.cooee.p.TokenUpdate
import com.baulsupp.okurl.authenticator.AuthInterceptor
import com.baulsupp.okurl.credentials.DefaultToken
import com.baulsupp.okurl.credentials.TokenSet

suspend fun Main.tokenResponse(request: TokenRequest): TokenResponse? {
  suspend fun <T> AuthInterceptor<T>.getTokenString(
    request: TokenRequest
  ): String? {
    val tokenSet = request.token_set?.let { TokenSet(it) } ?: DefaultToken
    val token = credentialsStore.get(serviceDefinition, tokenSet) ?: return null
    return serviceDefinition.formatCredentialsString(token)
  }

  val serviceName = request.service

  val service = services.find { it.name() == serviceName }
  val tokenString = service?.getTokenString(request)

  if (tokenString == null && request.login_url != null) {
    outputHandler.showError("Authenticating externally")

    outputHandler.openLink(request.login_url)

    return TokenResponse(login_attempted = true)
  }

  return TokenResponse(token = tokenString?.let { TokenUpdate(service = serviceName, token = tokenString) })
}
