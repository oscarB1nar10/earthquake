package com.example.earthquakealarm.data

/**
 * Immutable snapshot of everything the user can configure. Lives in the data
 * layer and is exposed reactively by [SettingsRepository]; the UI renders it
 * and the domain layer reads it when firing the alarm.
 */
data class AlarmSettings(
    val keywords: String = DEFAULT_KEYWORDS,
    val wifiEnabled: Boolean = true,
    // Base URL of the ESP32; endpoints /alarm, /off, /status hang off it.
    val esp32BaseUrl: String = DEFAULT_ESP32_BASE_URL,
    // Fire the alarm on any WhatsApp notification, sending this message.
    val whatsAppFireEnabled: Boolean = false,
    val whatsAppMessage: String = DEFAULT_WHATSAPP_MESSAGE,
) {
    /** Parsed, normalized keyword list used for matching. */
    fun keywordList(): List<String> =
        keywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    companion object {
        // Multilingual defaults covering Google's "Android Earthquake Alerts".
        const val DEFAULT_KEYWORDS = "Agáchate,Cúbrete,Agárrate,Drop,cover,hold on"
        const val DEFAULT_ESP32_BASE_URL = "http://192.168.1.10"
        const val DEFAULT_WHATSAPP_MESSAGE = "Suppose this is an earthquake notification"
    }
}
