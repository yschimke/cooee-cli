package com.baulsupp.cooee.cli

import com.baulsupp.oksocial.output.UsageException
import com.baulsupp.okurl.authenticator.AuthInterceptor
import com.baulsupp.okurl.commands.ToolSession
import com.baulsupp.okurl.credentials.TokenSet
import com.baulsupp.okurl.kotlin.query
import com.baulsupp.okurl.kotlin.queryForString
import com.baulsupp.okurl.kotlin.request
import com.baulsupp.okurl.secrets.Secrets

class ServiceAuthorisation(val main: ToolSession, val apiHost: String) {
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
    val token = auth.serviceDefinition.formatCredentialsString(credentials)

    main.credentialsStore.set(auth.serviceDefinition, tokenSet.name, credentials)
    main.client.queryForString(request("$apiHost/api/v0/authorize?serviceName=${auth.name()}&token=$token&tokenSet=${tokenSet.name}") {
      post(emptyBody)
    })
  }

  suspend fun <T> authRequest(auth: AuthInterceptor<T>, tokenSet: TokenSet) {
    auth.serviceDefinition.accountsLink()?.let { main.outputHandler.info("Accounts: $it") }

    val credentials = auth.authorize(main.client, main.outputHandler, listOf())

    saveCredentials(auth, tokenSet, credentials)

    Secrets.instance.saveIfNeeded()
  }
}
