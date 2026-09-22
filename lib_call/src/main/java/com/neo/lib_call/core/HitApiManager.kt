package com.neo.lib_call.core

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.neo.lib_call.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Created by Kharozim
 * 11/06/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. My Application
 * All Rights Reserved
 */
internal data class CallApiRequest(
  @SerializedName("number")
  val number: String,
  @SerializedName("device")
  val device: String,
  @SerializedName("agent_extension")
  val agenExtention: String,
  @SerializedName("param")
  val param: Map<String, String>,
)

internal data class BaseResponse<T>(
  val success: Boolean? = null,
  val message: String? = null,
  val data: T? = null,
)

internal data class CallResponse(
  val callId: String? = null,
  val finalNumber: String? = null,
)

internal object HitApiManager {

  private const val CALL_API_URL = "http://147.139.193.218/ami/api/v1/call"

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
//    .addInterceptor(HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) })
    .build()

  suspend fun hitCallApi(
    request: CallApiRequest,
  ): Result<CallResponse> = withContext(Dispatchers.IO) {
    try {
      val jsonBody = Gson().toJson(request)
      val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

      val httpRequest = Request.Builder()
        .url(CALL_API_URL)
        .post(body)
        .addHeader("Accept", "application/json")
        .build()

      Logger.d("hitCallApi request: ${httpRequest.method} ${httpRequest.url}")
      Logger.d("hitCallApi request payload: $jsonBody")

      client.newCall(httpRequest).execute().use { response ->
        val responseBody = response.body.string()

        Logger.d("hitCallApi response: ${response.code} ${response.message}")
        Logger.d("hitCallApi response body: $responseBody")

        if (response.isSuccessful) {
          try {
            val type = object : TypeToken<BaseResponse<CallResponse>>() {}.type
            val responseData: BaseResponse<CallResponse> = Gson().fromJson(responseBody, type)
            Result.success(responseData.data ?: CallResponse())
          } catch (e: Exception) {
            Result.failure(e)
          }
        } else {
          Result.failure(
            Exception("API Error ${response.code}: $responseBody")
          )
        }
      }

    } catch (e: Exception) {
      Logger.e("hitCallApi exception: ${e.message}", e)
      Result.failure(e)
    }
  }
}
