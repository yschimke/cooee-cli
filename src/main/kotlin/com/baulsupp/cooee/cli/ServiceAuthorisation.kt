package com.baulsupp.cooee.cli

import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.authenticator.AuthInterceptor
import com.baulsupp.okurl.commands.ToolSession
import com.baulsupp.okurl.credentials.TokenSet
import com.baulsupp.okurl.kotlin.client
import com.baulsupp.okurl.secrets.Secrets

class ServiceAuthorisation(val main: ToolSession) {

  suspend fun authorize(
    serviceName: String,
    token: String? = null,
    tokenSet: TokenSet
  ) {
    val auth = main.serviceLibrary.findAuthInterceptor(serviceName) ?: throw UsageException(
      "unable to find authenticator. Specify name from " + main.serviceLibrary.knownServices().joinToString(", ")
    )

    if (token != null) {
      storeCredentials(auth, token, tokenSet)
    } else {
      authRequest(auth, tokenSet)
    }
  }

  private suspend fun <T> storeCredentials(auth: AuthInterceptor<T>, token: String, tokenSet: TokenSet) {
    val credentials = auth.serviceDefinition.parseCredentialsString(token)
    saveCredentials(auth, tokenSet, credentials)
  }

  private suspend fun <T> saveCredentials(
    auth: AuthInterceptor<T>,
    tokenSet: TokenSet,
    credentials: T
  ) {
    main.credentialsStore.set(auth.serviceDefinition, tokenSet.name, credentials)
  }

  suspend fun <T> authRequest(auth: AuthInterceptor<T>, tokenSet: TokenSet) {
    auth.serviceDefinition.accountsLink()?.let { main.outputHandler.info("Accounts: $it") }

    val credentials = auth.authorize(client, main.outputHandler, listOf())

    main.credentialsStore.set(auth.serviceDefinition, tokenSet.name, credentials)

    Secrets.instance.saveIfNeeded()
  }
}
