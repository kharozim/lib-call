package com.neo.lib_call.core

import com.neo.lib_call.model.CallAudioState
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.RegisterState
import com.neo.lib_call.model.SpeakerOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object CallSessionManager {
  private val _callState = MutableStateFlow(CallState.Idle)
  private val _callStatusMessage = MutableStateFlow("Idle")
  private val _registerState = MutableStateFlow(RegisterState.None)
  private val _registerStatusMessage = MutableStateFlow("Not registered")
  private val _audioState = MutableStateFlow(CallAudioState())

  val callState: StateFlow<CallState> = _callState.asStateFlow()
  val callStatusMessage: StateFlow<String> = _callStatusMessage.asStateFlow()
  val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()
  val registerStatusMessage: StateFlow<String> = _registerStatusMessage.asStateFlow()
  val audioState: StateFlow<CallAudioState> = _audioState.asStateFlow()

  fun updateCallState(state: CallState, message: String) {
    _callState.value = state
    _callStatusMessage.value = message
  }

  fun updateRegisterState(state: RegisterState, message: String) {
    _registerState.value = state
    _registerStatusMessage.value = message
  }

  fun updateAudioState(audioState: CallAudioState) {
    _audioState.value = audioState
  }

  fun updateMuteState(isMuted: Boolean) {
    _audioState.value = _audioState.value.copy(isMicMuted = isMuted)
  }

  fun updateSpeakerState(selected: SpeakerOut?, available: List<SpeakerOut>) {
    _audioState.value = _audioState.value.copy(
      speakerOutput = selected,
      availableSpeakerOutputs = available,
    )
  }

  fun reset() {
    updateCallState(CallState.Idle, "Idle")
    updateRegisterState(RegisterState.None, "Not registered")
    _audioState.value = CallAudioState()
  }
}
