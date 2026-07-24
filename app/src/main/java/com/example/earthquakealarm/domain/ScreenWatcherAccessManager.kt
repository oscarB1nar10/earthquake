package com.example.earthquakealarm.domain

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.example.earthquakealarm.service.EarthquakeScreenWatcherService

/**
 * Answers whether the screen watcher accessibility service is currently
 * enabled in Settings → Accessibility. Mirrors [NotificationAccessManager].
 */
class ScreenWatcherAccessManager(context: Context) {

    private val appContext = context.applicationContext

    fun isEnabled(): Boolean {
        val manager = appContext.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager ?: return false
        val expected = ComponentName(appContext, EarthquakeScreenWatcherService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val info = it.resolveInfo?.serviceInfo
                info?.packageName == expected.packageName && info?.name == expected.className
            }
    }
}
