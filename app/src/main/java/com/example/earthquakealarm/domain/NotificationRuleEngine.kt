package com.example.earthquakealarm.domain

import com.example.earthquakealarm.data.AlarmSettings

/**
 * Runs the registered [NotificationRule]s in order and returns the first
 * [AlarmSignal] produced (first match wins). Add a rule in the
 * [Hilt module][com.example.earthquakealarm.di.AppModule] and it joins the chain.
 */
class NotificationRuleEngine(private val rules: List<NotificationRule>) {

    fun evaluate(
        notification: ReceivedNotification,
        settings: AlarmSettings,
    ): AlarmSignal? = rules.firstNotNullOfOrNull { it.evaluate(notification, settings) }
}
