package com.neo.lib_call.model

data class CallRequest(
  val destinationNumber: String,
  val destinationName: String?,
  val contactImage: String?,
  val metadata: Map<String, String>,
  @Deprecated("Registration is owned by CallSdk and is not part of a call request.")
  val credentials: SipCredentials? = null,
)
