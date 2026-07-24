package com.example.earthquakealarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earthquakealarm.data.EventLogRepository
import com.example.earthquakealarm.data.SettingsRepository
import com.example.earthquakealarm.domain.AlarmDispatcher
import com.example.earthquakealarm.domain.AlarmSignal
import com.example.earthquakealarm.domain.Esp32AlarmClient
import com.example.earthquakealarm.domain.Esp32Result
import com.example.earthquakealarm.domain.NotificationAccessManager
import com.example.earthquakealarm.domain.ScreenWatcherAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns and exposes [MainUiState]. Persistent settings and the event log come
 * from repositories as flows; "system state" (notification access, screen
 * watcher) is polled on demand because Android only lets us read it
 * imperatively. All three are merged into one immutable state.
 *
 * Constructed by Hilt (@HiltViewModel); the View calls the `on…` intent
 * methods and nothing flows the other way.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val eventLog: EventLogRepository,
    private val alarmDispatcher: AlarmDispatcher,
    private val notificationAccessManager: NotificationAccessManager,
    private val screenWatcherAccessManager: ScreenWatcherAccessManager,
    private val esp32AlarmClient: Esp32AlarmClient,
) : ViewModel() {

    private val systemState = MutableStateFlow(SystemState())

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.settings,
        eventLog.entries,
        systemState,
    ) { settings, events, system ->
        MainUiState(
            settings = settings,
            notificationAccessGranted = system.notificationAccess,
            screenWatcherEnabled = system.screenWatcher,
            events = events,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainUiState())

    init {
        refreshSystemState()
    }

    /** Re-reads imperative system state; call from the screen's ON_RESUME. */
    fun refreshSystemState() {
        systemState.value = SystemState(
            notificationAccess = notificationAccessManager.isEnabled(),
            screenWatcher = screenWatcherAccessManager.isEnabled(),
        )
    }

    // --- intents ---

    fun onKeywordsChange(value: String) =
        settingsRepository.update { it.copy(keywords = value) }

    fun onWifiEnabledChange(enabled: Boolean) =
        settingsRepository.update { it.copy(wifiEnabled = enabled) }

    fun onEsp32BaseUrlChange(value: String) =
        settingsRepository.update { it.copy(esp32BaseUrl = value) }

    fun onWhatsAppFireEnabledChange(enabled: Boolean) =
        settingsRepository.update { it.copy(whatsAppFireEnabled = enabled) }

    fun onWhatsAppMessageChange(value: String) =
        settingsRepository.update { it.copy(whatsAppMessage = value) }

    fun onTestTrigger() {
        viewModelScope.launch {
            alarmDispatcher.fire(AlarmSignal("manual test", AlarmSignal.DEFAULT_ALARM_MESSAGE))
        }
    }

    // --- direct ESP32 controls (talk to the device regardless of the transport toggle) ---

    fun onFireAlarm() = esp32Action("Fire /alarm") { esp32AlarmClient.triggerAlarm(it) }

    fun onStopAlarm() = esp32Action("Stop /off") { esp32AlarmClient.turnOff(it) }

    fun onCheckStatus() = esp32Action("Status /status") { esp32AlarmClient.status(it) }

    private fun esp32Action(label: String, call: suspend (baseUrl: String) -> Esp32Result) {
        val baseUrl = settingsRepository.settings.value.esp32BaseUrl
        viewModelScope.launch {
            when (val result = call(baseUrl)) {
                is Esp32Result.Ok -> {
                    val suffix = if (result.body.isNotEmpty()) " — ${result.body.take(60)}" else ""
                    eventLog.add("$label → OK$suffix")
                }
                is Esp32Result.Error -> eventLog.add("$label → FAILED (${result.reason})")
            }
        }
    }

    fun onClearLog() = eventLog.clear()

    /** Imperative system state, refreshed together. */
    private data class SystemState(
        val notificationAccess: Boolean = false,
        val screenWatcher: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
