package com.neo.lib_call.core

import android.content.Context
import com.neo.lib_call.model.CallAudioState
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.RegisterState
import com.neo.lib_call.model.SipCredentials
import com.neo.lib_call.model.SpeakerOut
import com.neo.lib_call.util.Logger
import java.security.MessageDigest
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.AuthInfo
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.ProxyConfig
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.VersionUpdateCheckResult

internal object LinphoneManager {
  private const val REGISTRATION_ATTEMPTS = 50
  private const val REGISTRATION_POLL_DELAY_MS = 100L

  private var initialized = false
  private var core: Core? = null
  private var activeCall: Call? = null
  private var activeCredentialFingerprint: String? = null
  private var activeSipAccount: RegisteredSipAccount? = null
  private var activeProxyDomain: String? = null
  private var activeAccount: Account? = null
  private var activeAuthInfo: AuthInfo? = null
  private var audioFocusManager: CallAudioManager? = null
  private val registrationMutex = Mutex()

  internal data class RegisteredSipAccount(
    val username: String,
    val domain: String,
  )

  private val listener = object : CoreListenerStub() {
    override fun onAccountRegistrationStateChanged(
      core: Core,
      account: Account,
      state: RegistrationState,
      message: String,
    ) {
      if (account.nativePointer != activeAccount?.nativePointer) return

      when (state) {
        RegistrationState.None -> {
          CallSessionManager.updateRegisterState(
            RegisterState.None,
            "Not registered",
          )
        }

        RegistrationState.Progress -> {
          CallSessionManager.updateRegisterState(
            RegisterState.Progress,
            "Registering SIP account",
          )
        }

        RegistrationState.Ok -> {
          CallSessionManager.updateRegisterState(RegisterState.Ok, "Registered")
        }

        RegistrationState.Cleared -> {
          CallSessionManager.updateRegisterState(
            RegisterState.Cleared,
            "Registration cleared",
          )
        }

        RegistrationState.Failed -> {
          CallSessionManager.updateRegisterState(
            RegisterState.Failed,
            "Registration failed",
          )
        }

        RegistrationState.Refreshing -> {
          CallSessionManager.updateRegisterState(
            RegisterState.Refreshing,
            "Refreshing registration",
          )
        }
      }
    }

    override fun onRegistrationStateChanged(
      core: Core,
      proxyConfig: ProxyConfig,
      state: RegistrationState?,
      message: String,
    ) {
      super.onRegistrationStateChanged(core, proxyConfig, state, message)

    }

    override fun onCallStateChanged(core: Core, call: Call, state: Call.State, message: String) {
      activeCall = call
      Logger.d("onCallStateChanged state=$state message=${message.ifBlank { "<blank>" }}")

      when (state) {
        Call.State.OutgoingInit -> {
          audioFocusManager?.requestRingingFocus()
          applyPreferredAudioRoute(core)
//          CallSessionManager.updateCallState(CallState.Dialing, message.ifBlank { "Dialing" })
        }

        Call.State.OutgoingProgress,
        Call.State.OutgoingRinging,
        Call.State.OutgoingEarlyMedia,
          -> {
          audioFocusManager?.requestRingingFocus()
          applyPreferredAudioRoute(core)
//          CallSessionManager.updateCallState(CallState.Ringing, message.ifBlank { "Ringing" })
        }

        Call.State.Connected, Call.State.StreamsRunning -> {
          audioFocusManager?.requestCallFocus()
//          CallSessionManager.updateCallState(CallState.Connected, message.ifBlank { "Connected" })
        }

        Call.State.End, Call.State.Error, Call.State.Released -> {
          val endedState = if (state == Call.State.Error) CallState.Failed else CallState.Ended
          CallSessionManager.updateCallState(endedState, message.ifBlank { endedState.name })
          if (state == Call.State.End || state == Call.State.Error || state == Call.State.Released) {
            activeCall = null
          }
          audioFocusManager?.releaseFocus()
        }

        Call.State.IncomingReceived -> {
          activeCall = call
          val params = core.createCallParams(call)
          if (params == null) {
            activeCall?.accept()
          } else {
            activeCall?.acceptWithParams(params)
          }
        }

        else -> Unit
      }

      refreshAudioState()
    }

  }

  fun initialize(context: Context, isDebug: Boolean) {
    if (initialized) return

    val factory = Factory.instance()
    factory.enableLogCollection(org.linphone.core.LogCollectionState.Enabled)

    val createdCore = factory.createCore(null, null, context.applicationContext)
    createdCore.addListener(listener)
    createdCore.isNetworkReachable = true
    createdCore.isEchoCancellationEnabled = true
    createdCore.isAgcEnabled = true
    createdCore.start()

    core = createdCore
    audioFocusManager = CallAudioManager(context.applicationContext)
    Logger.isLoggerActive = isDebug
    initialized = true

    Logger.d("Initial ringback=${createdCore.ringback}")
    Logger.d("Linphone audio devices=${createdCore.audioDevices.joinToString { it.type.name }}")
    Logger.d("Linphone output device=${createdCore.outputAudioDevice?.type?.name}")
    refreshAudioState()
    Logger.d("Linphone manager initialized")
  }

  suspend fun registerAccount(credentials: SipCredentials) = registrationMutex.withLock {
    require(initialized) { "LinphoneManager is not initialized." }
    require(credentials.username.isNotBlank()) { "username is required" }
    require(credentials.password.isNotBlank()) { "password is required" }
    require(credentials.domain.isNotBlank()) { "domain is required" }

    if (activeCredentialFingerprint == credentials.fingerprint() &&
      activeAccount?.state == RegistrationState.Ok
    ) {
      CallSessionManager.updateRegisterState(RegisterState.Ok, "Registered")
      refreshAudioState()
      return
    }

    val linphoneCore = requireNotNull(core) { "Linphone core is missing." }
    check(linphoneCore.callsNb == 0) {
      "Cannot change SIP account while a call is active."
    }

    removeActiveAccount(linphoneCore)
    CallSessionManager.updateRegisterState(RegisterState.Progress, "Registering SIP account")

    val normalizedDomain = credentials.domain
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
    linphoneCore.addAuthInfo(authInfo)

    val accountParams = linphoneCore.createAccountParams()
    accountParams.identityAddress = identity

    val serverAddress = Factory.instance().createAddress("sip:$normalizedDomain")
    serverAddress?.transport = TransportType.Udp
    accountParams.serverAddress = serverAddress
    accountParams.isRegisterEnabled = true

    val account = linphoneCore.createAccount(accountParams)
    activeCredentialFingerprint = credentials.fingerprint()
    activeSipAccount = RegisteredSipAccount(
      username = credentials.username,
      domain = normalizedDomain,
    )
    activeProxyDomain = normalizedDomain
    activeAccount = account
    activeAuthInfo = authInfo
    linphoneCore.addAccount(account)
    linphoneCore.defaultAccount = account
    linphoneCore.refreshRegisters()

    try {
      waitForRegistration()
      refreshAudioState()
    } catch (throwable: Throwable) {
      CallSessionManager.updateRegisterState(RegisterState.Failed, "Registration failed")
      throw throwable
    }
  }

  suspend fun unregisterAccount() = registrationMutex.withLock {
    require(initialized) { "LinphoneManager is not initialized." }
    val linphoneCore = requireNotNull(core) { "Linphone core is missing." }
    check(linphoneCore.callsNb == 0) {
      "Cannot unregister SIP account while a call is active."
    }

    val account = activeAccount
    if (account == null) {
      CallSessionManager.resetRegistration()
      return
    }

    val accountParams = account.params.clone()
    accountParams.isRegisterEnabled = false
    account.params = accountParams
    try {
      waitForUnregistration(account)
    } finally {
      removeActiveAccount(linphoneCore)
      CallSessionManager.resetRegistration()
    }
  }

  fun activeSipAccountOrNull(): RegisteredSipAccount? = activeSipAccount

  fun isRegisteredFor(credentials: SipCredentials): Boolean {
    return activeCredentialFingerprint == credentials.fingerprint() &&
      activeAccount?.state == RegistrationState.Ok
  }

  fun hasActiveCall(): Boolean = core?.callsNb?.let { it > 0 } == true

  private fun removeActiveAccount(linphoneCore: Core) {
    activeAccount?.let(linphoneCore::removeAccount)
    activeAuthInfo?.let(linphoneCore::removeAuthInfo)
    activeAccount = null
    activeAuthInfo = null
    activeCredentialFingerprint = null
    activeSipAccount = null
    activeProxyDomain = null
  }

  private fun SipCredentials.fingerprint(): String {
    val source = "$username\u0000$domain\u0000$password"
    return MessageDigest.getInstance("SHA-256")
      .digest(source.toByteArray(Charsets.UTF_8))
      .joinToString(separator = "") { byte -> "%02x".format(byte) }
  }

  fun startOutgoingCall(destinationNumber: String, phoneId: String? = null) {
    require(initialized) { "LinphoneManager is not initialized." }
    require(destinationNumber.isNotBlank()) { "destinationNumber is required" }

    val linphoneCore = requireNotNull(core) { "Linphone core is missing." }
    val domain = requireNotNull(activeProxyDomain) {
      "Linphone account is not registered. Call registerAccount(...) first."
    }

    CallSessionManager.updateCallState(CallState.Dialing, "Dialing $destinationNumber")
    val focusGranted = audioFocusManager?.requestRingingFocus() == true
    if (!focusGranted) {
      CallSessionManager.updateCallState(CallState.Failed, "Audio focus was not granted")
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
      phoneId?.let {
        params.addCustomHeader("X-Telphone_ID", it)
      }

      val call = linphoneCore.inviteAddressWithParams(address, params)
      activeCall = call
      if (call == null) {
        audioFocusManager?.releaseFocus()
        throw IllegalStateException("Linphone returned null call")
      }

      refreshAudioState()
//      waitForCallToConnect()
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
    refreshAudioState()
  }

  fun sendDtmf(value: String): Boolean {
    val digit = value.singleOrNull() ?: return false
    if (digit !in DTMF_DIGITS) return false

    val call = activeCall ?: return false
    if (call.state != Call.State.Connected && call.state != Call.State.StreamsRunning) {
      return false
    }

    val result = call.sendDtmf(digit)
    if (result != 0) {
      Logger.d("Failed to send DTMF digit=$digit result=$result")
      return false
    }

    Logger.d("Sent DTMF digit=$digit")
    return true
  }

  fun toggleMute(): Boolean {
    val call = activeCall ?: return false
    val newMuted = !call.microphoneMuted
    call.microphoneMuted = newMuted
    CallSessionManager.updateMuteState(newMuted)
    refreshAudioState()
    Logger.d("Toggled microphone mute muted=$newMuted")
    return newMuted
  }

  fun cycleSpeakerOutput(): SpeakerOut? {
    val linphoneCore = core ?: return null
    val availableOutputs = collectAvailableSpeakerOutputs(linphoneCore)
    if (availableOutputs.isEmpty()) {
      refreshAudioState()
      return null
    }

    val currentOutput = resolveSelectedSpeakerOutput(linphoneCore, activeCall)
    val currentIndex = availableOutputs.indexOf(currentOutput)
    val nextOutput = availableOutputs[(currentIndex + 1).mod(availableOutputs.size)]
    selectSpeakerOutput(nextOutput)
    return nextOutput
  }

  fun selectSpeakerOutput(output: SpeakerOut): SpeakerOut? {
    val linphoneCore = core ?: return null
    val audioDevice = findAudioDeviceForOutput(linphoneCore, output)
    if (audioDevice == null) {
      Logger.d("No Linphone audio device found for output=${output.name}")
      refreshAudioState()
      return null
    }

    linphoneCore.outputAudioDevice = audioDevice
    activeCall?.setOutputAudioDevice(audioDevice)
    Logger.d(
      "Selected speaker output=${output.name} device=${audioDevice.type.name} " +
        "callState=${activeCall?.state?.name}"
    )
    refreshAudioState()
    return output
  }

  fun refreshAudioState() {
    val linphoneCore = core
    if (linphoneCore == null) {
      CallSessionManager.updateAudioState(CallAudioState())
      return
    }

    val availableOutputs = collectAvailableSpeakerOutputs(linphoneCore)
    val selectedOutput = resolveSelectedSpeakerOutput(linphoneCore, activeCall)
    val isMicMuted = activeCall?.microphoneMuted ?: false

    CallSessionManager.updateAudioState(
      CallAudioState(
        isMicMuted = isMicMuted,
        speakerOutput = selectedOutput,
        availableSpeakerOutputs = availableOutputs,
      )
    )
  }

  private suspend fun waitForRegistration() {
    repeat(REGISTRATION_ATTEMPTS) {
      val state = activeAccount?.state

      if (state == RegistrationState.Ok) return
      if (state == RegistrationState.Failed) {
        throw IllegalStateException("SIP registration failed.")
      }
      delay(REGISTRATION_POLL_DELAY_MS)
    }

    if (activeAccount?.state != RegistrationState.Ok) {
      throw IllegalStateException("Timed out while waiting for registration.")
    }
  }

  private suspend fun waitForUnregistration(account: Account) {
    repeat(REGISTRATION_ATTEMPTS) {
      when (account.state) {
        RegistrationState.Cleared,
        RegistrationState.None,
          -> return

        else -> delay(REGISTRATION_POLL_DELAY_MS)
      }
    }

    throw IllegalStateException("Timed out while waiting for unregistration.")
  }

  private suspend fun waitForCallToConnect() {
    repeat(50) {
      when (activeCall?.state) {
        Call.State.Connected,
        Call.State.StreamsRunning,
          -> return

        Call.State.Error -> throw IllegalStateException("Call failed to connect.")
        else -> delay(100)
      }
    }

    throw IllegalStateException("Timed out while waiting for call to connect.")
  }

  private fun applyPreferredAudioRoute(linphoneCore: Core) {
    val currentOutput = linphoneCore.outputAudioDevice
    if (currentOutput?.type?.isExternalPreferredRoute() == true &&
      currentOutput.hasCapability(AudioDevice.Capabilities.CapabilityPlay)
    ) {
      Logger.d("Keeping current external output audio device=${currentOutput.type.name}")
      return
    }

    val preferredDevice =
      findAudioDeviceForOutput(linphoneCore, SpeakerOut.Bluethooth)
        ?: findAudioDeviceForOutput(linphoneCore, SpeakerOut.Headphone)
        ?: findAudioDeviceForOutput(linphoneCore, SpeakerOut.Earpiece)
        ?: findAudioDeviceForOutput(linphoneCore, SpeakerOut.LoadSpeaker)
        ?: linphoneCore.audioDevices.firstOrNull {
          it.hasCapability(AudioDevice.Capabilities.CapabilityPlay)
        }

    if (preferredDevice == null) {
      Logger.d("No preferred output audio device found")
      return
    }

    linphoneCore.outputAudioDevice = preferredDevice
    activeCall?.setOutputAudioDevice(preferredDevice)
    Logger.d("Selected output audio device=${preferredDevice.type.name}")
  }

  private fun collectAvailableSpeakerOutputs(linphoneCore: Core): List<SpeakerOut> {
    return linphoneCore.audioDevices.asSequence()
      .filter { it.hasCapability(AudioDevice.Capabilities.CapabilityPlay) }
      .mapNotNull { it.type.toSpeakerOutOrNull() }
      .distinct()
      .toList()
  }

  private fun resolveSelectedSpeakerOutput(core: Core, call: Call?): SpeakerOut? {
    val device = call?.outputAudioDevice ?: core.outputAudioDevice ?: return null
    return device.type.toSpeakerOutOrNull()
  }

  private fun findAudioDeviceForOutput(core: Core, output: SpeakerOut): AudioDevice? {
    val preferredTypes = when (output) {
      SpeakerOut.Earpiece -> listOf(AudioDevice.Type.Earpiece)
      SpeakerOut.LoadSpeaker -> listOf(AudioDevice.Type.Speaker)
      SpeakerOut.Bluethooth -> listOf(AudioDevice.Type.Bluetooth, AudioDevice.Type.BluetoothA2DP)
      SpeakerOut.Headphone -> listOf(AudioDevice.Type.Headset, AudioDevice.Type.Headphones)
    }

    return preferredTypes.asSequence()
      .mapNotNull { type ->
        core.audioDevices.firstOrNull { device ->
          device.type == type && device.hasCapability(AudioDevice.Capabilities.CapabilityPlay)
        }
      }
      .firstOrNull()
  }

  private fun AudioDevice.Type.toSpeakerOutOrNull(): SpeakerOut? {
    return when (this) {
      AudioDevice.Type.Earpiece -> SpeakerOut.Earpiece
      AudioDevice.Type.Speaker -> SpeakerOut.LoadSpeaker
      AudioDevice.Type.Bluetooth,
      AudioDevice.Type.BluetoothA2DP,
        -> SpeakerOut.Bluethooth

      AudioDevice.Type.Headset,
      AudioDevice.Type.Headphones,
        -> SpeakerOut.Headphone

      else -> null
    }
  }

  private fun AudioDevice.Type.isExternalPreferredRoute(): Boolean {
    return this == AudioDevice.Type.Bluetooth ||
      this == AudioDevice.Type.BluetoothA2DP ||
      this == AudioDevice.Type.Headset ||
      this == AudioDevice.Type.Headphones
  }

  private val DTMF_DIGITS = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#')
}
