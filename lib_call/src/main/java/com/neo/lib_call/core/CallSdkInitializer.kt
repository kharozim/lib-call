package com.neo.lib_call.core

import android.content.Context
import java.lang.ref.WeakReference

internal object CallSdkInitializer {
  fun initialize(contextRef: WeakReference<Context>, isDebug: Boolean) {
    ContextProvider.initialize(contextRef)
    LinphoneManager.initialize(ContextProvider.requireContext(), isDebug)
  }
}
