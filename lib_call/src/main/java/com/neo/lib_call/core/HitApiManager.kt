package com.neo.lib_call.core

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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
  @SerializedName("telephone_id")
  val telephoneId: String,
  @SerializedName("customer_id")
  val customerId: String,
  @SerializedName("agent_extension")
  val username: String,
  @SerializedName("customer_name")
  val customerName: String,
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

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) })
    .build()

  suspend fun hitCallApi(
    request: CallApiRequest,
  ): Result<CallResponse> = withContext(Dispatchers.IO) {
    try {
      val jsonBody = Gson().toJson(request)
      val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

      val httpRequest = Request.Builder()
        .url("https://api-dial.neokarya.co.id/ami/api/v1/call")
        .post(body)
        .addHeader("Accept", "application/json")
        .build()

      client.newCall(httpRequest).execute().use { response ->
        val responseBody = response.body?.string().orEmpty()

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
      Result.failure(e)
    }
  }
}