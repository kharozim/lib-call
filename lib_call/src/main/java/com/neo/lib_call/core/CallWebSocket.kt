package com.neo.lib_call.core

import android.util.Log
import com.neo.lib_call.model.CallState
import com.neo.lib_call.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Created by Kharozim
 * 15/06/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. Neo Call Sdk
 * All Rights Reserved
 */
internal data class WsRequest(
  val type: String,
  val username: String,
  val domain: String,
  val agentExtention: String,
)

internal data class WsResponse(
  val event: String? = null,
  val callId: String? = null,
  val duration: String? = null,
  val telephoneId: String? = null,
  val taskId: String? = null,
  val purpose: String? = null,
)

object CallWebSocket {
  private const val TAG = "WebSocketManager"

  private var webSocket: WebSocket? = null
  private val _callState = MutableStateFlow(CallState.Idle)

  val callState: StateFlow<CallState> = _callState.asStateFlow()

  private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .pingInterval(20, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }

  private var isConnected = false
  private var currentUrl: String? = null

  fun connectWebSocket(
    url: String,
    token: String? = null,
    onOpen: (() -> Unit)? = null,
    onMessage: ((String) -> Unit)? = null,
    onFailure: ((Throwable) -> Unit)? = null,
    onClosed: (() -> Unit)? = null,
  ) {
    if (isConnected && currentUrl == url) {
      Logger.d("WebSocket already connected")
      return
    }

    closeWebSocketConnection()

    currentUrl = url

    val requestBuilder = Request.Builder()
      .url(url)

//    if (!token.isNullOrBlank()) {
//      requestBuilder.addHeader("Authorization", "Bearer $token")
//    }

    val request = requestBuilder.build()

    webSocket = client.newWebSocket(
      request,
      object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
          super.onOpen(webSocket, response)
          Logger.d("WebSocket connected")
          isConnected = true
          onOpen?.invoke()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
          super.onMessage(webSocket, text)
          Logger.d("WebSocket message: $text")
          onMessage?.invoke(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
          super.onClosing(webSocket, code, reason)
          Logger.d("WebSocket closing: $code / $reason")
          webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          super.onClosed(webSocket, code, reason)
          Logger.d("WebSocket closed: $code / $reason")
          isConnected = false
          currentUrl = null
          onClosed?.invoke()
        }

        override fun onFailure(
          webSocket: WebSocket,
          t: Throwable,
          response: Response?,
        ) {
          super.onFailure(webSocket, t, response)
          Log.e(TAG, "WebSocket failure: ${t.message}", t)
          isConnected = false
          currentUrl = null
          onFailure?.invoke(t)
        }
      }
    )
  }

  fun sendMessage(message: String): Boolean {
    val socket = webSocket

    if (socket == null || !isConnected) {
      Log.w(TAG, "Cannot send message, WebSocket not connected")
      return false
    }

    return socket.send(message)
  }

  fun closeWebSocketConnection(
    code: Int = 1000,
    reason: String = "Close connection WebSocket",
  ) {
    Logger.d("Close connection WebSocket")

    try {
      webSocket?.close(code, reason)
      webSocket = null
      isConnected = false
      currentUrl = null
    } catch (e: Exception) {
      Log.e(TAG, "Failed to unsubscribe WebSocket", e)
    }
  }

  fun updateCallState(state: CallState) {
    _callState.value = state
  }

  fun isWebSocketConnected(): Boolean {
    return isConnected
  }
}