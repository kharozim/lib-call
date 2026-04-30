package com.neo.lib_call.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Created by Kharozim
 * 30/04/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. My Application
 * All Rights Reserved
 */
internal class TimerManager {
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var timerJob: Job? = null

  private val _formattedTime = MutableStateFlow("")
  val formattedTime = _formattedTime.asStateFlow()

  /**
   * Memulai penghitungan waktu dari 00:00
   */
  fun startTimer() {
    if (timerJob?.isActive == true) return

    // Reset waktu ke awal setiap kali start baru
    _formattedTime.value = "00:00"

    timerJob = scope.launch {
      var seconds = 0L
      while (isActive) {
        _formattedTime.value = formatTime(seconds)
        delay(1000)
        seconds++
      }
    }
  }

  /**
   * Menghentikan perhitungan saja.
   * Nilai terakhir di [formattedTime] akan tetap bertahan agar bisa ditampilkan di UI.
   */
  fun stopTimer() {
    timerJob?.cancel()
    timerJob = null
  }

  /**
   * Mereset tampilan kembali ke 00:00 secara manual.
   */
  fun resetTimer() {
    _formattedTime.value = "00:00"
  }

  /**
   * Mengonversi detik ke format mm:ss
   */
  private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
  }
}
