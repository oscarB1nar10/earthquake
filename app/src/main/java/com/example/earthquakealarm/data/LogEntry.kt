package com.example.earthquakealarm.data

/**
 * One line in the activity log. Carries the raw timestamp (not a formatted
 * string) so formatting stays a UI concern.
 */
data class LogEntry(
    val timestampMillis: Long,
    val message: String,
)
