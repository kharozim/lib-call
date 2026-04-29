package com.neo.lib_call.core

import android.content.Context
import java.lang.ref.WeakReference

internal object ContextProvider {
  private var applicationContext: Context? = null

  fun initialize(contextRef: WeakReference<Context>) {
    val resolvedContext = requireNotNull(contextRef.get()) {
      "CallSdk.init failed because the provided WeakReference<Context> is empty."
    }
    applicationContext = resolvedContext.applicationContext
  }

  fun requireContext(): Context {
    return requireNotNull(applicationContext) {
      "CallSdk has not been initialized. Call CallSdk.init(...) first."
    }
  }
}
