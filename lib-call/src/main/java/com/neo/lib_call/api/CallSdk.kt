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

  fun init(contextRef: WeakReference<Context>) {
    CallSdkInitializer.initialize(contextRef)
    initialized = true
  }

  fun makeCall(
    context: Context,
    destinationNumber: String,
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
