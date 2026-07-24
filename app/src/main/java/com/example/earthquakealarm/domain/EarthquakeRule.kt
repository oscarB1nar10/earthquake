package com.example.earthquakealarm.domain

import com.example.earthquakealarm.data.AlarmSettings

/**
 * The primary rule: fires on Google's earthquake alerts, matched by keyword in
 * the notification text via [EarthquakeMatcher].
 */
class EarthquakeRule(private val matcher: EarthquakeMatcher) : NotificationRule {

    override fun evaluate(
        notification: ReceivedNotification,
        settings: AlarmSettings,
    ): AlarmSignal? {
        val matched = matcher.firstMatch(notification.text, settings.keywordList()) ?: return null
        return AlarmSignal(
            reason = "earthquake '$matched' in ${notification.packageName}",
            message = AlarmSignal.DEFAULT_ALARM_MESSAGE,
        )
    }
}
