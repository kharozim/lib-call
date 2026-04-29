package com.neo.lib_call.model

enum class CallState {
  Idle,
  Initializing,
  Registering,
  Registered,
  RegistrationFailed,
  Dialing,
  Ringing,
  Connected,
  Ended,
  Failed
}
