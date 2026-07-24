package com.example.earthquakealarm.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive store for [AlarmSettings]. Depending on an interface (not the
 * SharedPreferences implementation) keeps the ViewModel and domain layer
 * testable and lets us swap the backing store (e.g. DataStore) later.
 */
interface SettingsRepository {
    val settings: StateFlow<AlarmSettings>

    /** Applies [transform] to the current settings, persists, and emits. */
    fun update(transform: (AlarmSettings) -> AlarmSettings)
}
