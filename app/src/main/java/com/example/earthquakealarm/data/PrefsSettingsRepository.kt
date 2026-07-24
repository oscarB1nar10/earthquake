package com.example.earthquakealarm.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed [SettingsRepository]. Keeps an in-memory
 * [MutableStateFlow] mirror so reads are synchronous (the notification
 * listener needs them on the spot) while writes both persist and emit.
 */
class PrefsSettingsRepository(context: Context) : SettingsRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readFromPrefs())
    override val settings: StateFlow<AlarmSettings> = _settings.asStateFlow()

    override fun update(transform: (AlarmSettings) -> AlarmSettings) {
        val updated = transform(_settings.value)
        writeToPrefs(updated)
        _settings.value = updated
    }

    private fun readFromPrefs() = AlarmSettings(
        keywords = prefs.getString(KEY_KEYWORDS, AlarmSettings.DEFAULT_KEYWORDS)
            ?: AlarmSettings.DEFAULT_KEYWORDS,
        wifiEnabled = prefs.getBoolean(KEY_WIFI_ENABLED, true),
        esp32BaseUrl = prefs.getString(KEY_ESP32_BASE_URL, AlarmSettings.DEFAULT_ESP32_BASE_URL)
            ?: AlarmSettings.DEFAULT_ESP32_BASE_URL,
        whatsAppFireEnabled = prefs.getBoolean(KEY_WA_ENABLED, false),
        whatsAppMessage = prefs.getString(KEY_WA_MSG, AlarmSettings.DEFAULT_WHATSAPP_MESSAGE)
            ?: AlarmSettings.DEFAULT_WHATSAPP_MESSAGE,
    )

    private fun writeToPrefs(s: AlarmSettings) = prefs.edit {
        putString(KEY_KEYWORDS, s.keywords)
        putBoolean(KEY_WIFI_ENABLED, s.wifiEnabled)
        putString(KEY_ESP32_BASE_URL, s.esp32BaseUrl)
        putBoolean(KEY_WA_ENABLED, s.whatsAppFireEnabled)
        putString(KEY_WA_MSG, s.whatsAppMessage)
    }

    private companion object {
        const val PREFS_NAME = "eq_alarm_prefs"
        const val KEY_KEYWORDS = "keywords"
        const val KEY_WIFI_ENABLED = "wifi_enabled"
        const val KEY_ESP32_BASE_URL = "esp32_base_url"
        const val KEY_WA_ENABLED = "wa_enabled"
        const val KEY_WA_MSG = "wa_msg"
    }
}
