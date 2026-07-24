package com.example.earthquakealarm.ui

import com.example.earthquakealarm.data.AlarmSettings
import com.example.earthquakealarm.data.LogEntry

/**
 * The complete, immutable description of what the screen shows at any moment.
 * The View is a pure function of this; the [MainViewModel] is its only producer.
 */
data class MainUiState(
    val settings: AlarmSettings = AlarmSettings(),
    val notificationAccessGranted: Boolean = false,
    val screenWatcherEnabled: Boolean = false,
    val events: List<LogEntry> = emptyList(),
)
