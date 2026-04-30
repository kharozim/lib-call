package com.neo.lib_call.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Created by Kharozim
 * 21/04/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. SIPApp
 * All Rights Reserved
 */
@Composable
internal fun SetSystemBarAppearance(
  isLight: Boolean = true,
) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      val controller = WindowCompat.getInsetsController(window, window.decorView)
      controller.isAppearanceLightStatusBars = isLight
      controller.isAppearanceLightNavigationBars = isLight
    }
  }
}