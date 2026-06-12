package com.neo.lib_call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neo.lib_call.core.CallApiRequest
import com.neo.lib_call.core.CallSessionManager
import com.neo.lib_call.core.HitApiManager
import com.neo.lib_call.core.LinphoneManager
import com.neo.lib_call.core.RegisterUseCase
import com.neo.lib_call.core.TimerManager
import com.neo.lib_call.model.CallRequest
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.RegisterState
import com.neo.lib_call.model.SpeakerOut
import com.neo.lib_call.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CallUiState(
  val destinationNumber: String = "",
  val destinationName: String? = null,
  val contactImage: String? = null,
  val metadata: Map<String, String> = emptyMap(),
  val callState: CallState = CallState.Idle,
  val callStateMessage: String = "Idle",
  val registerState: RegisterState = RegisterState.None,
  val registerStateMessage: String = "Not registered",
  val isMicMuted: Boolean = false,
  val speakerOutput: SpeakerOut? = null,
  val availableSpeakerOutputs: List<SpeakerOut> = emptyList(),
  val fatalError: String? = null,
  val timeCall: String = "",
)

internal class CallViewModel(
  private val request: CallRequest,
  private val timerManager: TimerManager,
  private val registerUseCase: RegisterUseCase = RegisterUseCase(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(
    CallUiState(
      destinationNumber = request.destinationNumber,
      destinationName = request.destinationName,
      contactImage = request.contactImage,
      metadata = request.metadata,
    )
  )
  val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

  init {
    observeCallSession()
    observeTimerSession()
  }

  private fun observeTimerSession() {
    viewModelScope.launch {
      timerManager.formattedTime.collect { timeCall ->
        _uiState.update { it.copy(timeCall = timeCall) }
      }
    }
  }

  private fun observeCallSession() {
    viewModelScope.launch {
      CallSessionManager.callState.collect { state ->
        _uiState.update { current -> current.copy(callState = state) }
        when (state) {
          CallState.Connected -> timerManager.startTimer()
          CallState.Ended -> timerManager.stopTimer()
          CallState.Failed -> timerManager.stopTimer()
          else -> Unit
        }
      }
    }
    viewModelScope.launch {
      CallSessionManager.callStatusMessage.collect { message ->
        _uiState.update { current -> current.copy(callStateMessage = message) }
      }
    }
    viewModelScope.launch {
      CallSessionManager.registerState.collect { state ->
        _uiState.update { current -> current.copy(registerState = state) }
      }
    }
    viewModelScope.launch {
      CallSessionManager.registerStatusMessage.collect { message ->
        _uiState.update { current -> current.copy(registerStateMessage = message) }
      }
    }
    viewModelScope.launch {
      CallSessionManager.audioState.collect { audioState ->
        _uiState.update { current ->
          current.copy(
            isMicMuted = audioState.isMicMuted,
            speakerOutput = audioState.speakerOutput,
            availableSpeakerOutputs = audioState.availableSpeakerOutputs,
          )
        }
      }
    }
  }

  private fun startCall() {
    viewModelScope.launch {
      try {
        CallSessionManager.updateCallState(CallState.Initializing, "Preparing call")
        CallSessionManager.updateRegisterState(RegisterState.Progress, "Registering SIP account")
        registerUseCase.register(request.credentials)
        CallSessionManager.updateCallState(CallState.Dialing, "Calling")

//        LinphoneManager.startOutgoingCall(request.destinationNumber, request.metadata["phone_id"])
        val startCall = HitApiManager.hitCallApi(
          CallApiRequest(
            number = request.destinationNumber,
            telephoneId = request.metadata["phone_id"].orEmpty(),
            customerId = "2",
            username = request.credentials.username,
            customerName = uiState.value.destinationName.orEmpty()
          )
        )
        startCall.onFailure { throwable ->
          throw throwable
        }
      } catch (throwable: Throwable) {
        Logger.e("Unable to start SIP call", throwable)
        if (CallSessionManager.registerState.value != RegisterState.Ok) {
          CallSessionManager.updateRegisterState(
            RegisterState.Failed,
            throwable.message ?: "Registration failed"
          )
        }
        CallSessionManager.updateCallState(
          CallState.Failed,
          throwable.message ?: "Failed to start SIP call"
        )
        _uiState.update { current ->
          current.copy(fatalError = throwable.message ?: "Failed to start SIP call")
        }
      }
    }
  }

  fun beginCall() {
    if (_uiState.value.callState == CallState.Initializing ||
      _uiState.value.callState == CallState.Dialing
    ) {
      return
    }
    startCall()
  }

  fun setFatalError(message: String) {
    CallSessionManager.updateCallState(CallState.Failed, message)
    _uiState.update { current -> current.copy(fatalError = message) }
  }

  fun endCall() {
    LinphoneManager.endCall()
  }

  fun toggleMute() {
    viewModelScope.launch {
      LinphoneManager.toggleMute()
    }
  }

  fun cycleSpeakerOutput() {
    viewModelScope.launch {
      LinphoneManager.cycleSpeakerOutput()
    }
  }

  fun sendDtmf(value: String) {
    viewModelScope.launch {
      val sent = LinphoneManager.sendDtmf(value)
      if (!sent) {
        Logger.d("DTMF ignored value=$value state=${_uiState.value.callState}")
      }
    }
  }

  fun selectSpeakerOutput(output: SpeakerOut) {
    viewModelScope.launch {
      LinphoneManager.selectSpeakerOutput(output)
    }
  }

  override fun onCleared() {
    super.onCleared()
    CallSessionManager.reset()
  }

  class Factory(
    private val request: CallRequest,
    private val timerManager: TimerManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
        return CallViewModel(request, timerManager = timerManager) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
