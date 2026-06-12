package com.neo.lib_call.model

enum class CallState {
  Idle,
  Initializing,
  Dialing,
  Ringing,
  Connected,
  Ended,
  Failed
}

enum class RegisterState {
  None,
  Progress,
  Ok,
  Cleared,
  Failed,
  Refreshing,
}
