package com.example.earthquakealarm.domain

import com.example.earthquakealarm.data.EventLogRepository
import com.example.earthquakealarm.data.SettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Turns "an earthquake was detected" into signals on every enabled transport.
 * Fires them in parallel (so one slow transport never delays another) and
 * records each outcome in the event log.
 *
 * A short re-entry cooldown collapses bursts (several WhatsApp messages, a
 * notification the system re-posts) into one trigger, and is the second line
 * of defence against feedback loops — the first being that the notification
 * listener ignores this app's own notifications.
 */
class AlarmDispatcher(
    private val transports: List<AlarmTransport>,
    private val settingsRepository: SettingsRepository,
    private val eventLog: EventLogRepository,
    private val notifier: AlarmNotifier,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
) {

    private val lock = Any()
    private var lastFiredNanos = 0L

    /** Suspends until every enabled transport has finished; returns their results. */
    suspend fun fire(signal: AlarmSignal): List<TransportResult> = coroutineScope {
        if (!tryAcquireCooldown()) {
            eventLog.add("Suppressed (cooldown ${cooldownMs / 1000}s) — ${signal.reason}")
            return@coroutineScope emptyList()
        }

        val settings = settingsRepository.settings.value
        eventLog.add("⚡ TRIGGER — ${signal.reason} → \"${signal.message}\"")

        // Surface a local notification regardless of which transports are on.
        notifier.show(signal)

        val active = transports.filter { it.isEnabled(settings) }
        if (active.isEmpty()) {
            eventLog.add("No transport enabled — nothing sent")
            return@coroutineScope emptyList()
        }

        active
            .map { transport ->
                async { transport.fire(settings, signal.message).also { eventLog.add(it.toLogMessage()) } }
            }
            .awaitAll()
    }

    /** Thread-safe check-and-set: true if enough time passed since the last fire. */
    private fun tryAcquireCooldown(): Boolean {
        val now = System.nanoTime() // Monotonic — immune to wall-clock jumps.
        synchronized(lock) {
            if (lastFiredNanos != 0L && now - lastFiredNanos < cooldownMs * 1_000_000L) {
                return false
            }
            lastFiredNanos = now
            return true
        }
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 10_000L
    }
}
