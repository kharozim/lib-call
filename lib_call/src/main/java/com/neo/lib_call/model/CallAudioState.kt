package com.neo.lib_call.model

internal data class CallAudioState(
  val isMicMuted: Boolean = false,
  val speakerOutput: SpeakerOut? = null,
  val availableSpeakerOutputs: List<SpeakerOut> = emptyList(),
)
