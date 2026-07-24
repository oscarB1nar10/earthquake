package com.example.earthquakealarm.domain

import com.example.earthquakealarm.data.AlarmSettings

/** A posted notification reduced to just the fields the rules inspect. */
data class ReceivedNotification(
    val packageName: String,
    val text: String,
)

/**
 * Strategy: decides whether a notification should fire the alarm and, if so,
 * with what [AlarmSignal]. Returning null means "not my concern" so the next
 * rule gets a turn. Mirrors the [AlarmTransport] pattern on the output side.
 */
interface NotificationRule {
    fun evaluate(notification: ReceivedNotification, settings: AlarmSettings): AlarmSignal?
}
