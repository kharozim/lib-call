package com.neo.lib_call.util

import android.util.Log

internal object Logger {
  private const val TAG = "CallSdk"
  var isLoggerActive = true

  fun d(message: String) {
    if (!isLoggerActive) return
    Log.d(TAG, message)
  }

  fun e(message: String, throwable: Throwable? = null) {
    if (!isLoggerActive) return
    Log.e(TAG, message, throwable)
  }
}
