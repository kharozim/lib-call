package com.neo.lib_call.util

import android.util.Log

internal object Logger {
  private const val TAG = "CallSdk"

  fun d(message: String) {
    Log.d(TAG, message)
  }

  fun e(message: String, throwable: Throwable? = null) {
    Log.e(TAG, message, throwable)
  }
}
