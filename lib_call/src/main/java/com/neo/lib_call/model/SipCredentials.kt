package com.neo.lib_call.model

/**
 * Credentials used to register one SIP account with [com.neo.lib_call.api.CallSdk.register].
 *
 * Integrators should obtain these values from secure runtime storage and must not log them.
 */
data class SipCredentials(
  val username: String,
  val password: String,
  val domain: String,
)
