package com.baulsupp.cooee.cli

import com.baulsupp.okurl.services.AbstractServiceDefinition

data class Jwt(val token: String)

val serviceDefinition = object : AbstractServiceDefinition<Jwt>(
  "coo.ee", "Cooee API", "cooee",
  "https://coo.ee", "https://coo.ee"
) {
  override fun parseCredentialsString(s: String): Jwt = Jwt(s)

  override fun formatCredentialsString(credentials: Jwt): String = credentials.token
}
