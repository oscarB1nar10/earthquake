package com.example.earthquakealarm.domain

import com.example.earthquakealarm.data.AlarmSettings

/** Which physical link delivered (or tried to deliver) the alarm. */
enum class TransportType(val label: String) {
    WIFI("WiFi"),
}

/** Outcome of a single transport attempt. */
sealed interface TransportResult {
    val type: TransportType

    data class Success(override val type: TransportType, val detail: String) : TransportResult
    data class Failure(override val type: TransportType, val reason: String) : TransportResult
}

/** Human-readable line for the activity log. */
fun TransportResult.toLogMessage(): String = when (this) {
    is TransportResult.Success -> "${type.label} → OK ($detail)"
    is TransportResult.Failure -> "${type.label} → FAILED ($reason)"
}

/**
 * Strategy pattern: each implementation is one way of signalling the
 * microcontroller. New links (USB, MQTT, …) just add another implementation and
 * register it in the [Hilt module][com.example.earthquakealarm.di.AppModule];
 * nothing else changes.
 */
interface AlarmTransport {
    val type: TransportType

    /** Whether the user has this transport switched on. */
    fun isEnabled(settings: AlarmSettings): Boolean

    /**
     * Delivers [message] to the microcontroller. Must not throw — failures come
     * back as [TransportResult.Failure].
     */
    suspend fun fire(settings: AlarmSettings, message: String): TransportResult
}
