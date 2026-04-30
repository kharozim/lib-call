package com.neo.lib_call.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.neo.lib_call.util.Logger

internal class CallAudioManager(context: Context) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private var activeFocusType = FocusType.None
  private var audioFocusRequest: AudioFocusRequest? = null
  private var previousAudioMode: Int = AudioManager.MODE_NORMAL
  private var previousAudioModeSaved = false

  private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
    when (change) {
      AudioManager.AUDIOFOCUS_GAIN -> {
        Logger.d("Audio focus gained; activeFocusType=$activeFocusType mode=${audioManager.mode}")
      }
      AudioManager.AUDIOFOCUS_LOSS -> {
        Logger.d("Audio focus lost; activeFocusType=$activeFocusType mode=${audioManager.mode}")
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        Logger.d("Audio focus transient loss; activeFocusType=$activeFocusType mode=${audioManager.mode}")
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        Logger.d("Audio focus ducked; activeFocusType=$activeFocusType mode=${audioManager.mode}")
      }
      else -> {
        Logger.d("Audio focus change=$change; activeFocusType=$activeFocusType mode=${audioManager.mode}")
      }
    }
  }

  fun requestRingingFocus(): Boolean {
    logAudioSnapshot("requestRingingFocus")
    if (activeFocusType == FocusType.Ringing || activeFocusType == FocusType.Call) {
      Logger.d("Ringing focus already active; activeFocusType=$activeFocusType mode=${audioManager.mode}")
      ensureCommunicationMode("ringing focus already active")
      return true
    }

    Logger.d("Requesting ringing audio focus; mode=${audioManager.mode}")
    val granted = requestFocus(
      gainType = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
      usage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
      contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
    )

    if (granted) {
      savePreviousModeIfNeeded()
      ensureCommunicationMode("ringing focus granted")
      activeFocusType = FocusType.Ringing
      Logger.d("Ringing audio focus granted; mode=${audioManager.mode}")
    } else {
      Logger.d("Ringing audio focus failed; mode=${audioManager.mode}")
    }
    return granted
  }

  fun requestCallFocus(): Boolean {
    logAudioSnapshot("requestCallFocus")
    if (activeFocusType == FocusType.Call) {
      Logger.d("Call focus already active; mode=${audioManager.mode}")
      return true
    }

    Logger.d("Requesting call audio focus; mode=${audioManager.mode}")
    abandonCurrentFocus(restoreMode = false)

    val granted = requestFocus(
      gainType = AudioManager.AUDIOFOCUS_GAIN,
      usage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
      contentType = AudioAttributes.CONTENT_TYPE_SPEECH,
    )

    if (granted) {
      savePreviousModeIfNeeded()
      audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
      activeFocusType = FocusType.Call
      Logger.d(
        "Call audio focus granted; previousMode=$previousAudioMode currentMode=${audioManager.mode}"
      )
    } else {
      Logger.d("Call audio focus failed; mode=${audioManager.mode}")
    }
    return granted
  }

  fun releaseFocus() {
    Logger.d("Releasing audio focus; activeFocusType=$activeFocusType mode=${audioManager.mode}")
    logAudioSnapshot("releaseFocus")
    abandonCurrentFocus(restoreMode = true)
  }

  private fun requestFocus(
    gainType: Int,
    usage: Int,
    contentType: Int,
  ): Boolean {
    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val request = AudioFocusRequest.Builder(gainType)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(contentType)
            .build()
        )
        .setOnAudioFocusChangeListener(focusChangeListener)
        .setWillPauseWhenDucked(true)
        .build()

      audioFocusRequest = request
      audioManager.requestAudioFocus(request)
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(
        focusChangeListener,
        AudioManager.STREAM_VOICE_CALL,
        gainType
      )
    }

    Logger.d("Audio focus request result=$result activeFocusType=$activeFocusType mode=${audioManager.mode}")
    return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  private fun abandonCurrentFocus(restoreMode: Boolean) {
    if (activeFocusType == FocusType.None && audioFocusRequest == null) {
      if (restoreMode && previousAudioModeSaved) {
        restorePreviousMode()
      }
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { request ->
        audioManager.abandonAudioFocusRequest(request)
      }
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(focusChangeListener)
    }

    audioFocusRequest = null
    activeFocusType = FocusType.None

    if (restoreMode) {
      restorePreviousMode()
    }
  }

  private fun savePreviousModeIfNeeded() {
    if (!previousAudioModeSaved) {
      previousAudioMode = audioManager.mode
      previousAudioModeSaved = true
      Logger.d("Saved previous audio mode=$previousAudioMode")
    }
  }

  private fun restorePreviousMode() {
    if (previousAudioModeSaved) {
      Logger.d("Restoring audio mode from ${audioManager.mode} to $previousAudioMode")
      audioManager.mode = previousAudioMode
      previousAudioModeSaved = false
      previousAudioMode = AudioManager.MODE_NORMAL
    }
  }

  private fun ensureCommunicationMode(reason: String) {
    savePreviousModeIfNeeded()
    if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
      Logger.d(
        "Switching audio mode to MODE_IN_COMMUNICATION; reason=$reason previous=${audioManager.mode}"
      )
      audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }
  }

  private fun logAudioSnapshot(reason: String) {
    Logger.d(
      "Audio snapshot reason=$reason mode=${audioManager.mode} speakerphone=${audioManager.isSpeakerphoneOn} " +
        "voiceVolume=${audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)} " +
        "ringVolume=${audioManager.getStreamVolume(AudioManager.STREAM_RING)} " +
        "musicVolume=${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}"
    )
  }

  private enum class FocusType {
    None,
    Ringing,
    Call
  }
}
