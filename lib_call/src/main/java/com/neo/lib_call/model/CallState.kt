package com.neo.lib_call.model

/** Lifecycle state of one call session. */
enum class CallState {
  Idle,
  Initializing,
  Dialing,
  Ringing,
  Connected,
  Ended,
  Failed
}

/** SIP account registration state mapped from Linphone registration callbacks. */
enum class RegisterState {
  None,
  Progress,
  Ok,
  Cleared,
  Failed,
  Refreshing,
}
