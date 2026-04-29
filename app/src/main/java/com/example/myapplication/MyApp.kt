package com.example.myapplication

import android.app.Application
import com.neo.lib_call.api.CallSdk

/**
 * Created by Kharozim
 * 29/04/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. My Application
 * All Rights Reserved
 */
class MyApp : Application() {
  override fun onCreate() {
    super.onCreate()
    CallSdk.init(this)
  }
}