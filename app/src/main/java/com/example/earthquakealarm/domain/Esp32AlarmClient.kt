package com.example.earthquakealarm.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Result of an ESP32 HTTP call. */
sealed interface Esp32Result {
    data class Ok(val body: String) : Esp32Result
    data class Error(val reason: String) : Esp32Result
}

/**
 * HTTP client for the ESP32 alarm firmware. Encapsulates the device's endpoints
 * so both the automatic [WifiTransport] and the manual UI controls speak to it
 * the same way:
 *
 *   GET  {base}/alarm[?msg=…]   → sound the alarm
 *   GET  {base}/off             → silence it
 *   GET  {base}/status          → query state
 *
 * Requires `android:usesCleartextTraffic="true"` since the ESP32 serves plain HTTP.
 */
class Esp32AlarmClient {

    suspend fun triggerAlarm(baseUrl: String, message: String? = null): Esp32Result =
        get(endpoint(baseUrl, "/alarm", message))

    suspend fun turnOff(baseUrl: String): Esp32Result =
        get(endpoint(baseUrl, "/off"))

    suspend fun status(baseUrl: String): Esp32Result =
        get(endpoint(baseUrl, "/status"))

    private fun endpoint(baseUrl: String, path: String, message: String? = null): String {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return ""
        val url = "$base$path"
        return if (message.isNullOrEmpty()) url
        else "$url?msg=" + URLEncoder.encode(message, "UTF-8")
    }

    private suspend fun get(urlText: String): Esp32Result = withContext(Dispatchers.IO) {
        if (urlText.isBlank()) return@withContext Esp32Result.Error("no URL set")
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(urlText).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("X-Source", "EarthquakeAlarm")
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                Esp32Result.Ok(body)
            } else {
                Esp32Result.Error("HTTP $code")
            }
        } catch (e: Exception) {
            Esp32Result.Error(e.message ?: "connection error")
        } finally {
            conn?.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 4000
    }
}
