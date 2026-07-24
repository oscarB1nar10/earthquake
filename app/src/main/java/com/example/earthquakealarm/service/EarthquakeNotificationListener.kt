package com.example.earthquakealarm.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.earthquakealarm.data.EventLogRepository
import com.example.earthquakealarm.data.SettingsRepository
import com.example.earthquakealarm.domain.AlarmDispatcher
import com.example.earthquakealarm.domain.NotificationRuleEngine
import com.example.earthquakealarm.domain.ReceivedNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The notification interceptor. Once the user grants "Notification access",
 * Android binds to this service and delivers every posted notification here.
 *
 * A thin adapter: it extracts the text, delegates the decision to the
 * [NotificationRuleEngine], and hands any resulting signal to [AlarmDispatcher].
 * Dependencies are field-injected by Hilt (@AndroidEntryPoint) during onCreate.
 */
@AndroidEntryPoint
class EarthquakeNotificationListener : NotificationListenerService() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var ruleEngine: NotificationRuleEngine
    @Inject lateinit var dispatcher: AlarmDispatcher
    @Inject lateinit var eventLog: EventLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        eventLog.add("Listener connected — watching notifications")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        eventLog.add("Listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Never react to our own notifications: the alarm notification embeds the
        // matched keyword in its text, so processing it would re-match and re-fire
        // forever (notification → fire → notification → …).
        if (sbn.packageName == packageName) return

        val received = ReceivedNotification(
            packageName = sbn.packageName,
            text = sbn.notification.extractText(),
        )
        val signal = ruleEngine.evaluate(received, settings.settings.value) ?: return

        eventLog.add("🔔 MATCH — ${signal.reason}")
        scope.launch { dispatcher.fire(signal) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Concatenates the visible text fields of a notification for matching. */
    private fun Notification.extractText(): String {
        val extras = this.extras ?: return ""
        return listOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
        ).joinToString(" ") { extras.getCharSequence(it)?.toString().orEmpty() }
    }
}
