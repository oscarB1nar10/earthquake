package com.example.earthquakealarm.domain

import com.example.earthquakealarm.data.AlarmSettings

/**
 * WiFi transport: sounds the alarm on the ESP32 over HTTP by hitting its
 * `/alarm` endpoint through [Esp32AlarmClient]. The alarm message is passed
 * along as a query param for the firmware to log if it wants.
 */
class WifiTransport(private val client: Esp32AlarmClient) : AlarmTransport {

    override val type = TransportType.WIFI

    override fun isEnabled(settings: AlarmSettings) = settings.wifiEnabled

    override suspend fun fire(settings: AlarmSettings, message: String): TransportResult {
        val base = settings.esp32BaseUrl
        if (base.isBlank()) return TransportResult.Failure(type, "no URL set")

        return when (val result = client.triggerAlarm(base, message)) {
            is Esp32Result.Ok -> TransportResult.Success(type, "$base/alarm")
            is Esp32Result.Error -> TransportResult.Failure(type, result.reason)
        }
    }
}
