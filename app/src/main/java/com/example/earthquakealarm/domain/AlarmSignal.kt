package com.example.earthquakealarm.domain

/**
 * The result of a notification matching a rule: a human [reason] for the log and
 * the [message] payload sent to the microcontroller. Carrying the message here
 * lets different notifications fire different text (earthquake vs. test).
 */
data class AlarmSignal(
    val reason: String,
    val message: String,
) {
    companion object {
        /** Default payload for a real alarm / the manual test button. */
        const val DEFAULT_ALARM_MESSAGE = "ALARM"
    }
}
