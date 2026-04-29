package com.neo.lib_call.model

enum class CallState {
  Idle,
  Initializing,
  Registering,
  Dialing,
  Ringing,
  Connected,
  Ended,
  Failed
}
