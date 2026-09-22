package com.neo.lib_call.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.neo.lib_call.core.CallSdkInitializer
import com.neo.lib_call.core.CallSessionManager
import com.neo.lib_call.core.LinphoneManager
import com.neo.lib_call.model.CallRequest
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.RegisterState
import com.neo.lib_call.model.SipCredentials
import com.neo.lib_call.ui.CallActivity
import com.neo.lib_call.util.Logger
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Entry point for initializing the SDK, registering a SIP account, and starting calls.
 *
 * Registration belongs to the SDK lifecycle and remains active when a call screen closes.
 */
object CallSdk {
  @Volatile
  private var initialized = false

  private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  const val IS_CALL_CONNECT = "IS_CALL_CONNECT"
  const val CALL_DETAIL_RESULT = "CALL_DETAIL_RESULT"

  /** Current SIP registration state, sourced from Linphone account callbacks. */
  val registrationState = CallSessionManager.registerState

  /** Current lifecycle state of the active call session. */
  val callState = CallSessionManager.callState

  /**
   * Initializes the Linphone core once for the application process.
   *
   * This does not register a SIP account. Call [register] separately after credentials are
   * available.
   */
  fun init(context: Context, isDebug: Boolean) {
    val weakPreference: WeakReference<Context> = WeakReference(context.applicationContext)
    CallSdkInitializer.initialize(weakPreference, isDebug)
    initialized = true
  }

  /**
   * Registers a SIP account independently from any call screen.
   *
   * Re-registering the same active account is idempotent. The result fails when validation,
   * registration, or its timeout fails. Credential values are never included in SDK logs.
   */
  suspend fun register(credentials: SipCredentials): Result<Unit> {
    return try {
      checkInitialized()
      withContext(Dispatchers.Main.immediate) {
        LinphoneManager.registerAccount(credentials)
      }
      Result.success(Unit)
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (throwable: Throwable) {
      Logger.e("Unable to register SIP account", throwable)
      Result.failure(throwable)
    }
  }

  /**
   * Unregisters the active SIP account.
   *
   * Unregister fails while a call is active. A successful call leaves the SDK unregistered until
   * [register] is called again.
   */
  suspend fun unregister(): Result<Unit> {
    return try {
      checkInitialized()
      withContext(Dispatchers.Main.immediate) {
        LinphoneManager.unregisterAccount()
      }
      Result.success(Unit)
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (throwable: Throwable) {
      Logger.e("Unable to unregister SIP account", throwable)
      Result.failure(throwable)
    }
  }

  /**
   * Starts a call using the SIP account previously registered through [register].
   *
   * This function fails immediately when registration is not ready or another call is active.
   */
  fun makeCall(
    context: Context,
    destinationNumber: String,
    destinationName: String? = null,
    contactImage: String? = null,
    metadata: Map<String, String> = emptyMap(),
  ) {
    val request = createRegisteredCallRequest(
      destinationNumber = destinationNumber,
      destinationName = destinationName,
      contactImage = contactImage,
      metadata = metadata,
    )
    startCallActivity(context, request)
  }

  /**
   * Creates an intent for a call using the SIP account previously registered through [register].
   *
   * The returned intent contains call data only and never contains SIP credentials.
   */
  fun makeCallIntent(
    context: Context,
    destinationNumber: String,
    destinationName: String? = null,
    contactImage: String? = null,
    metadata: Map<String, String> = emptyMap(),
  ): Intent {
    val request = createRegisteredCallRequest(
      destinationNumber = destinationNumber,
      destinationName = destinationName,
      contactImage = contactImage,
      metadata = metadata,
    )
    return CallActivity.createIntent(context, request).withNewTaskFlagWhenNeeded(context)
  }

  /**
   * Compatibility API that registers the supplied account asynchronously before starting a call.
   *
   * New integrations should call [register] explicitly and use the credential-free [makeCall].
   */
  @Deprecated(
    message = "Call CallSdk.register(...) first, then use makeCall(...) without credentials.",
  )
  fun makeCall(
    context: Context,
    destinationNumber: String,
    destinationName: String? = null,
    contactImage: String? = null,
    metadata: Map<String, String> = emptyMap(),
    username: String,
    password: String,
    domain: String,
  ) {
    checkInitialized()
    require(destinationNumber.isNotBlank()) { "destinationNumber is required" }
    val credentials = validatedCredentials(username, password, domain)
    val applicationContext = context.applicationContext

    sdkScope.launch {
      val result = register(credentials)
      if (result.isFailure) return@launch

      withContext(Dispatchers.Main) {
        makeCall(
          context = applicationContext,
          destinationNumber = destinationNumber,
          destinationName = destinationName,
          contactImage = contactImage,
          metadata = metadata,
        )
      }
    }
  }

  /**
   * Compatibility API for callers that still pass credentials when creating an intent.
   *
   * Because intent creation is synchronous, the matching account must already be registered.
   * New integrations should call [register] first and use the credential-free overload.
   */
  @Deprecated(
    message = "Call CallSdk.register(...) first, then use makeCallIntent(...) without credentials.",
  )
  fun makeCallIntent(
    context: Context,
    destinationNumber: String,
    destinationName: String? = null,
    contactImage: String? = null,
    metadata: Map<String, String> = emptyMap(),
    username: String,
    password: String,
    domain: String,
  ): Intent {
    checkInitialized()
    val credentials = validatedCredentials(username, password, domain)
    check(LinphoneManager.isRegisteredFor(credentials)) {
      "The supplied SIP account is not registered. Call CallSdk.register(...) first."
    }
    return makeCallIntent(
      context = context,
      destinationNumber = destinationNumber,
      destinationName = destinationName,
      contactImage = contactImage,
      metadata = metadata,
    )
  }

  private fun createRegisteredCallRequest(
    destinationNumber: String,
    destinationName: String?,
    contactImage: String?,
    metadata: Map<String, String>,
  ): CallRequest {
    checkInitialized()
    require(destinationNumber.isNotBlank()) { "destinationNumber is required" }
    check(registrationState.value == RegisterState.Ok &&
      LinphoneManager.activeSipAccountOrNull() != null
    ) {
      "SIP account is not registered. Call CallSdk.register(...) first."
    }
    val currentCallState = callState.value
    check(!LinphoneManager.hasActiveCall() && currentCallState in listOf(
      CallState.Idle,
      CallState.Ended,
      CallState.Failed,
    )) {
      "Another call is already active."
    }

    return CallRequest(
      destinationNumber = destinationNumber,
      destinationName = destinationName,
      contactImage = contactImage,
      metadata = metadata,
    )
  }

  private fun startCallActivity(context: Context, request: CallRequest) {
    context.startActivity(
      CallActivity.createIntent(context, request).withNewTaskFlagWhenNeeded(context)
    )
  }

  private fun Intent.withNewTaskFlagWhenNeeded(context: Context): Intent = apply {
    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  private fun validatedCredentials(
    username: String,
    password: String,
    domain: String,
  ): SipCredentials {
    require(username.isNotBlank()) { "username is required" }
    require(password.isNotBlank()) { "password is required" }
    require(domain.isNotBlank()) { "domain is required" }
    return SipCredentials(username = username, password = password, domain = domain)
  }

  private fun checkInitialized() {
    check(initialized) {
      "CallSdk has not been initialized. Call CallSdk.init(...) first."
    }
  }
}
