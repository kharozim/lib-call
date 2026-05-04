package com.neo.lib_call.core

import com.neo.lib_call.model.CallAudioState
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.SpeakerOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object CallSessionManager {
  private val _callState = MutableStateFlow(CallState.Idle)
  private val _statusMessage = MutableStateFlow("Idle")
  private val _audioState = MutableStateFlow(CallAudioState())

  val callState: StateFlow<CallState> = _callState.asStateFlow()
  val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
  val audioState: StateFlow<CallAudioState> = _audioState.asStateFlow()

  fun update(state: CallState, message: String) {
    _callState.value = state
    _statusMessage.value = message
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
    update(CallState.Idle, "Idle")
    _audioState.value = CallAudioState()
  }
}
