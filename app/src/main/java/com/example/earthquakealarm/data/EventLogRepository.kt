package com.example.earthquakealarm.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Observable, bounded activity log shared across the app. A single instance is
 * provided by the [Hilt module][com.example.earthquakealarm.di.AppModule] to the
 * services and the ViewModel.
 */
class EventLogRepository(private val maxEntries: Int = 60) {

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun add(message: String) {
        val entry = LogEntry(System.currentTimeMillis(), message)
        _entries.update { (listOf(entry) + it).take(maxEntries) }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
