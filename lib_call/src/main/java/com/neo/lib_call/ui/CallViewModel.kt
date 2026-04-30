package com.neo.lib_call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neo.lib_call.core.CallSessionManager
import com.neo.lib_call.core.LinphoneManager
import com.neo.lib_call.core.RegisterUseCase
import com.neo.lib_call.core.TimerManager
import com.neo.lib_call.model.CallRequest
import com.neo.lib_call.model.CallState
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
  val statusMessage: String = "Idle",
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
      CallSessionManager.statusMessage.collect { message ->
        _uiState.update { current -> current.copy(statusMessage = message) }
      }
    }
  }

  private fun startCall() {
    viewModelScope.launch {
      try {
        CallSessionManager.update(CallState.Initializing, "Preparing call")
        registerUseCase.register(request.credentials)
        LinphoneManager.startOutgoingCall(request.destinationNumber)
      } catch (throwable: Throwable) {
        Logger.e("Unable to start SIP call", throwable)
        CallSessionManager.update(
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
      _uiState.value.callState == CallState.Registering ||
      _uiState.value.callState == CallState.Dialing
    ) {
      return
    }
    startCall()
  }

  fun setFatalError(message: String) {
    CallSessionManager.update(CallState.Failed, message)
    _uiState.update { current -> current.copy(fatalError = message) }
  }

  fun endCall() {
    LinphoneManager.endCall()
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
