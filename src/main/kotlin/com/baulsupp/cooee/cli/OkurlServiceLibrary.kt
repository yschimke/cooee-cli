package com.baulsupp.cooee.cli

import com.baulsupp.okurl.authenticator.AuthInterceptor
import com.baulsupp.okurl.services.ServiceLibrary
import java.util.ServiceLoader

object OkurlServiceLibrary : ServiceLibrary {
  override val services: Iterable<AuthInterceptor<*>> by lazy {
    ServiceLoader.load(AuthInterceptor::class.java, AuthInterceptor::class.java.classLoader)
      .sortedBy { -it.priority }
  }

  override fun findAuthInterceptor(name: String): AuthInterceptor<*>? = services.find { it.name() == name }

  override fun knownServices(): Set<String> = services.map(AuthInterceptor<*>::name).toSortedSet()
}
