package com.neo.lib_call.core

import com.neo.lib_call.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Kharozim
 * 11/06/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. My Application
 * All Rights Reserved
 */
internal data class CallApiRequest(
  val number: String,
  val telephoneId: String,
  val customerId: String,
  val username: String,
  val customerName: String,
)

internal object HitApiManager {

  suspend fun hitCallApi(
    request: CallApiRequest,
  ): Result<String> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null

    try {
      val apiUrl = "http://149.129.218.243:3456/ami/api/v1/call"
      val url = URL(apiUrl)

      val jsonBody = """
            {
                "number": "${request.number}",
                "telephone_id": "${request.telephoneId}",
                "customer_id": "${request.customerId}",
                "agent_extension": "${request.username}",
                "customer_name": "${request.customerName}"
            }
        """.trimIndent()
      Logger.e("try call : $jsonBody")

      connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "POST"
      connection.connectTimeout = 15_000
      connection.readTimeout = 15_000
      connection.doOutput = true
      connection.doInput = true

      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Accept", "application/json")

      OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(jsonBody)
        writer.flush()
      }

      val responseCode = connection.responseCode

      val responseBody = if (responseCode in 200..299) {
        connection.inputStream.bufferedReader().use(BufferedReader::readText)
      } else {
        connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
          ?: "HTTP Error $responseCode"
      }

      if (responseCode in 200..299) {
        Logger.e("call : success $responseBody")
        Result.success(responseBody)
      } else {
        Logger.e("failure call : api error $responseBody")
        Result.failure(Exception("API Error $responseCode: $responseBody"))
      }

    } catch (e: Exception) {
      Logger.e("failure call :${e.localizedMessage}")
      Result.failure(e)
    } finally {
      Logger.e("call : disconnect")
      connection?.disconnect()
    }
  }
}