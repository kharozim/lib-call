package com.neo.lib_call.core

import android.content.Context
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.SipCredentials
import com.neo.lib_call.util.Logger
import kotlinx.coroutines.delay

internal object LinphoneManager {
  private var initialized = false

  fun initialize(context: Context) {
    if (initialized) return

    context.applicationContext
    initialized = true
    Logger.d("Linphone manager initialized")
  }

  suspend fun registerAccount(credentials: SipCredentials) {
    require(initialized) { "LinphoneManager is not initialized." }
    require(credentials.username.isNotBlank()) { "username is required" }
    require(credentials.password.isNotBlank()) { "password is required" }
    require(credentials.domain.isNotBlank()) { "domain is required" }

    CallSessionManager.update(CallState.Registering, "Registering SIP account")
    delay(400)
  }

  suspend fun startOutgoingCall(destinationNumber: String) {
    require(initialized) { "LinphoneManager is not initialized." }
    require(destinationNumber.isNotBlank()) { "destinationNumber is required" }

    CallSessionManager.update(CallState.Dialing, "Dialing $destinationNumber")
    delay(500)
    CallSessionManager.update(CallState.Ringing, "Ringing")
    delay(700)
    CallSessionManager.update(CallState.Connected, "Connected")
  }

  fun endCall() {
    if (!initialized) return
    CallSessionManager.update(CallState.Ended, "Call ended")
  }
}
