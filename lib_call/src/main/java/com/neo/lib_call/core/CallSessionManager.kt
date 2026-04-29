package com.neo.lib_call.core

import com.neo.lib_call.model.CallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object CallSessionManager {
  private val _callState = MutableStateFlow(CallState.Idle)
  private val _statusMessage = MutableStateFlow("Idle")

  val callState: StateFlow<CallState> = _callState.asStateFlow()
  val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

  fun update(state: CallState, message: String) {
    _callState.value = state
    _statusMessage.value = message
  }

  fun reset() {
    update(CallState.Idle, "Idle")
  }
}
