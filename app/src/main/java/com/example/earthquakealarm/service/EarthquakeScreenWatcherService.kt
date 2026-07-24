package com.example.earthquakealarm.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.earthquakealarm.data.EventLogRepository
import com.example.earthquakealarm.data.SettingsRepository
import com.example.earthquakealarm.domain.AlarmDispatcher
import com.example.earthquakealarm.domain.AlarmSignal
import com.example.earthquakealarm.domain.EarthquakeMatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Second detection path, complementing [EarthquakeNotificationListener].
 *
 * Google's strongest "Take Action" earthquake alert is a FULL-SCREEN takeover
 * (not necessarily a notification), so a notification listener can miss it.
 * This accessibility service is told by the system whenever a window from the
 * alert-capable packages (see res/xml/accessibility_config.xml: Google Play
 * services, Personal Safety, cell-broadcast receivers) appears or changes; it
 * reads the window's visible text and fires the alarm when it matches the
 * user's earthquake keywords.
 *
 * The package filter lives in the config XML, so ordinary apps (browsers, news)
 * never even reach this code — a news headline about an earthquake cannot
 * trigger it. Dependencies are field-injected by Hilt (@AndroidEntryPoint).
 */
@AndroidEntryPoint
class EarthquakeScreenWatcherService : AccessibilityService() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var matcher: EarthquakeMatcher
    @Inject lateinit var dispatcher: AlarmDispatcher
    @Inject lateinit var eventLog: EventLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastScanMs = 0L
    private var lastFiredMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        eventLog.add("Screen watcher connected — watching for full-screen alerts")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFiredMs < FIRE_COOLDOWN_MS) return // Already fired for this alert.
        if (now - lastScanMs < SCAN_THROTTLE_MS) return  // Windows spam events; scan at most ~2/s.
        lastScanMs = now

        val packageName = event.packageName?.toString() ?: return
        val text = buildString {
            event.text.forEach { append(it).append(' ') }
            append(collectText(event.source ?: rootInActiveWindow))
        }
        if (text.isBlank()) return

        val matched = matcher.firstMatch(text, settings.settings.value.keywordList()) ?: return
        lastFiredMs = now

        val signal = AlarmSignal(
            reason = "full-screen alert '$matched' in $packageName",
            message = AlarmSignal.DEFAULT_ALARM_MESSAGE,
        )
        eventLog.add("SCREEN MATCH — ${signal.reason}")
        scope.launch { dispatcher.fire(signal) }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Breadth-first walk of the window's node tree, bounded so it stays cheap. */
    private fun collectText(root: AccessibilityNodeInfo?): String {
        root ?: return ""
        val sb = StringBuilder()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            node.text?.let { sb.append(it).append(' ') }
            node.contentDescription?.let { sb.append(it).append(' ') }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::add)
            }
        }
        return sb.toString()
    }

    private companion object {
        const val SCAN_THROTTLE_MS = 500L
        const val FIRE_COOLDOWN_MS = 30_000L
        const val MAX_NODES = 250
    }
}
