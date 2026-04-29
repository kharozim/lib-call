package com.neo.lib_call.core

import com.neo.lib_call.model.SipCredentials

internal class SipAccountRegistrar(
  private val linphoneManager: LinphoneManager = LinphoneManager,
) {
  suspend fun register(credentials: SipCredentials) {
    linphoneManager.registerAccount(credentials)
  }
}
