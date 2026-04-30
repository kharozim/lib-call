package com.neo.lib_call.model

data class CallRequest(
  val destinationNumber: String,
  val destinationName: String?,
  val contactImage: String?,
  val metadata: Map<String, String>,
  val credentials: SipCredentials,
)
