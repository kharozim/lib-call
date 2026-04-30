package com.neo.lib_call.api

import android.content.Context
import android.content.Intent
import com.neo.lib_call.core.CallSdkInitializer
import com.neo.lib_call.model.CallRequest
import com.neo.lib_call.model.SipCredentials
import com.neo.lib_call.ui.CallActivity
import java.lang.ref.WeakReference

object CallSdk {
  @Volatile
  private var initialized = false

  fun init(context: Context) {
    val weakPreference: WeakReference<Context> = WeakReference(context.applicationContext)
    CallSdkInitializer.initialize(weakPreference)
    initialized = true
  }

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
    check(initialized) {
      "CallSdk has not been initialized. Call CallSdk.init(...) before makeCall(...)."
    }
    require(destinationNumber.isNotBlank()) { "destinationNumber is required" }
    require(username.isNotBlank()) { "username is required" }
    require(password.isNotBlank()) { "password is required" }
    require(domain.isNotBlank()) { "domain is required" }

    val request = CallRequest(
      destinationNumber = destinationNumber,
      destinationName = destinationName,
      contactImage = contactImage,
      metadata = metadata,
      credentials = SipCredentials(
        username = username,
        password = password,
        domain = domain,
      ),
    )

    val intent = CallActivity.createIntent(context, request).apply {
      if (context !is android.app.Activity) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
    context.startActivity(intent)
  }
}
