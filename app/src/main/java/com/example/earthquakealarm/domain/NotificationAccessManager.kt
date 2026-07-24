package com.example.earthquakealarm.domain

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.example.earthquakealarm.service.EarthquakeNotificationListener

/**
 * Answers the one question the UI needs about notification access: is our
 * listener currently enabled in Settings → Notification access?
 */
class NotificationAccessManager(context: Context) {

    private val appContext = context.applicationContext

    fun isEnabled(): Boolean {
        val expected = ComponentName(appContext, EarthquakeNotificationListener::class.java)
        val flat = Settings.Secure.getString(
            appContext.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":")
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it == expected }
    }
}
