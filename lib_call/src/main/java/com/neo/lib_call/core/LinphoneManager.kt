package com.neo.lib_call.core

import android.content.Context
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.SipCredentials
import com.neo.lib_call.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

internal object LinphoneManager {
  private val scope = CoroutineScope(Dispatchers.Default + Job())

  private var initialized = false
  private var core: Core? = null
  private var activeCall: Call? = null
  private var activeCredentials: SipCredentials? = null
  private var activeProxyDomain: String? = null
  private var activeAccount: Account? = null
  private var audioFocusManager: CallAudioManager? = null

  private val listener = object : CoreListenerStub() {
    override fun onAccountRegistrationStateChanged(
      core: Core,
      account: Account,
      state: RegistrationState,
      message: String,
    ) {
      when (state) {
        RegistrationState.Progress -> {
          CallSessionManager.update(
            CallState.Registering,
            message.ifBlank { "Registering SIP account" })
        }

        RegistrationState.Ok -> {
          CallSessionManager.update(CallState.Registered, message.ifBlank { "Registered" })
        }

        RegistrationState.Failed -> {
          CallSessionManager.update(
            CallState.RegistrationFailed,
            message.ifBlank { "Registration failed" }
          )
        }

        else -> Unit
      }
    }

    override fun onCallStateChanged(core: Core, call: Call, state: Call.State, message: String) {
      activeCall = call
      Logger.d("onCallStateChanged state=$state message=${message.ifBlank { "<blank>" }}")
      when (state) {
        Call.State.OutgoingInit -> {
          audioFocusManager?.requestRingingFocus()
          applyPreferredAudioRoute(core)
          CallSessionManager.update(CallState.Dialing, message.ifBlank { "Dialing" })
        }

        Call.State.OutgoingProgress,
        Call.State.OutgoingRinging,
        Call.State.OutgoingEarlyMedia,
          -> {
          audioFocusManager?.requestRingingFocus()
          applyPreferredAudioRoute(core)
          CallSessionManager.update(CallState.Ringing, message.ifBlank { "Ringing" })
        }

        Call.State.Connected, Call.State.StreamsRunning -> {
          audioFocusManager?.requestCallFocus()
          CallSessionManager.update(CallState.Connected, message.ifBlank { "Connected" })
        }

        Call.State.End, Call.State.Error, Call.State.Released -> {
          val endedState = if (state == Call.State.Error) CallState.Failed else CallState.Ended
          CallSessionManager.update(endedState, message.ifBlank { endedState.name })
          activeCall = null
          audioFocusManager?.releaseFocus()
        }

        else -> Unit
      }
    }
  }

  fun initialize(context: Context) {
    if (initialized) return

    val factory = Factory.instance()
//    factory.setDebugMode(true, "CallSdk")
    factory.enableLogCollection(org.linphone.core.LogCollectionState.Enabled)

    val createdCore = factory.createCore(null, null, context.applicationContext)
    createdCore.addListener(listener)
    createdCore.isNetworkReachable = true
    createdCore.start()
    core = createdCore
    audioFocusManager = CallAudioManager(context.applicationContext)
    initialized = true
    Logger.d("Initial ringback=${createdCore.ringback}")
    Logger.d("Linphone audio devices=${createdCore.audioDevices.joinToString { it.type.name }}")
    Logger.d("Linphone output device=${createdCore.outputAudioDevice?.type?.name}")
    Logger.d("Linphone manager initialized")
  }

  suspend fun registerAccount(credentials: SipCredentials) {
    require(initialized) { "LinphoneManager is not initialized." }
    require(credentials.username.isNotBlank()) { "username is required" }
    require(credentials.password.isNotBlank()) { "password is required" }
    require(credentials.domain.isNotBlank()) { "domain is required" }

    if (activeCredentials == credentials && activeAccount?.state == RegistrationState.Ok) {
      CallSessionManager.update(CallState.Registered, "Registered")
      return
    }

    val linphoneCore = requireNotNull(core) { "Linphone core is missing." }
    CallSessionManager.update(CallState.Registering, "Registering SIP account")

    val normalizedDomain = normalizeDomain(credentials.domain)
    val identity = requireNotNull(
      Factory.instance().createAddress("sip:${credentials.username}@$normalizedDomain")
    ) {
      "Unable to create SIP identity address."
    }
    val authInfo = Factory.instance().createAuthInfo(
      credentials.username,
      null,
      credentials.password,
      null,
      null,
      normalizedDomain
    )
    linphoneCore.clearAllAuthInfo()
    linphoneCore.addAuthInfo(authInfo)

    val accountParams = linphoneCore.createAccountParams()
    accountParams.identityAddress = identity

    val serverAddress = Factory.instance().createAddress("sip:$normalizedDomain")
    serverAddress?.transport = TransportType.Udp
    accountParams.serverAddress = serverAddress
    accountParams.isRegisterEnabled = true

    val account = linphoneCore.createAccount(accountParams)
    linphoneCore.addAccount(account)
    linphoneCore.defaultAccount = account
    linphoneCore.refreshRegisters()

    activeCredentials = credentials
    activeProxyDomain = normalizedDomain
    activeAccount = account
    waitForRegistration()
  }

  suspend fun startOutgoingCall(destinationNumber: String) {
    require(initialized) { "LinphoneManager is not initialized." }
    require(destinationNumber.isNotBlank()) { "destinationNumber is required" }

    val linphoneCore = requireNotNull(core) { "Linphone core is missing." }
    val domain = requireNotNull(activeProxyDomain) {
      "Linphone account is not registered. Call registerAccount(...) first."
    }
    CallSessionManager.update(CallState.Dialing, "Dialing $destinationNumber")
    val focusGranted = audioFocusManager?.requestRingingFocus() == true
    if (!focusGranted) {
      CallSessionManager.update(CallState.Failed, "Audio focus was not granted")
      throw IllegalStateException("Audio focus was not granted")
    }

    val address =
      requireNotNull(Factory.instance().createAddress("sip:$destinationNumber@$domain")) {
        "Unable to create SIP destination address."
      }
    try {
      applyPreferredAudioRoute(linphoneCore)

      val params = requireNotNull(linphoneCore.createCallParams(null)) {
        "Unable to create Linphone call params"
      }
      params.mediaEncryption = MediaEncryption.None
      params.disableRinging(false)

      val call = linphoneCore.inviteAddressWithParams(address, params)
      activeCall = call
      if (call == null) {
        audioFocusManager?.releaseFocus()
        throw IllegalStateException("Linphone returned null call")
      }
      waitForCallToConnect()
    } catch (throwable: Throwable) {
      audioFocusManager?.releaseFocus()
      throw throwable
    }
  }

  fun endCall() {
    if (!initialized) return
    val linphoneCore = core ?: return
    linphoneCore.terminateAllCalls()
    activeCall = null
    audioFocusManager?.releaseFocus()
    CallSessionManager.update(CallState.Ended, "Call ended")
  }

  private suspend fun waitForRegistration() {
    repeat(50 * 2) {
      val state = activeAccount?.state

      if (state == RegistrationState.Ok) return
      if (state == RegistrationState.Failed) {
        throw IllegalStateException("SIP registration failed.")
      }
      delay(100)
    }
    if (activeAccount?.state != RegistrationState.Ok) {
      throw IllegalStateException("Timed out while waiting for SIP registration.")
    }
  }

  private suspend fun waitForCallToConnect() {
    repeat(50) {
      when (activeCall?.state) {
        Call.State.Connected, Call.State.StreamsRunning -> return
        Call.State.Error -> throw IllegalStateException("Call failed to connect.")
        else -> delay(100)
      }
    }
  }

  private fun normalizeDomain(domain: String): String {
    return domain.removePrefix("sip://").removePrefix("sip:").trim()
  }

  private fun applyPreferredAudioRoute(linphoneCore: Core) {
    val audioDevice = linphoneCore.audioDevices.firstOrNull {
      it.type == AudioDevice.Type.Earpiece
    } ?: linphoneCore.audioDevices.firstOrNull {
      it.type == AudioDevice.Type.Speaker
    }

    if (audioDevice == null) {
      Logger.d("No preferred output audio device found")
      return
    }

    linphoneCore.outputAudioDevice = audioDevice
    Logger.d("Selected output audio device=${audioDevice.type.name}")
  }
}
