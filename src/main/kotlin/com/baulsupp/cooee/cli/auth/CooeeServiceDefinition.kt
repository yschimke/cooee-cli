package com.baulsupp.cooee.cli.auth

import com.baulsupp.okurl.credentials.ServiceDefinition

object CooeeServiceDefinition: ServiceDefinition<String> {
  override fun apiHost(): String = "coo.ee"

  override fun formatCredentialsString(credentials: String): String = credentials

  override fun parseCredentialsString(s: String): String = s

  override fun serviceName(): String = "Coo.ee"

  override fun shortName(): String = "cooee"
}
