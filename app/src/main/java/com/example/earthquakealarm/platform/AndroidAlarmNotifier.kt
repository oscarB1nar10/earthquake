package com.example.earthquakealarm.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.earthquakealarm.R
import com.example.earthquakealarm.domain.AlarmNotifier
import com.example.earthquakealarm.domain.AlarmSignal
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android implementation of [AlarmNotifier]. Creates a high-importance channel
 * once and posts one heads-up notification per trigger, with the fired message
 * as the headline and the reason as the body.
 */
class AndroidAlarmNotifier(context: Context) : AlarmNotifier {

    private val appContext = context.applicationContext
    private val nextId = AtomicInteger(1000)

    init {
        createChannel()
    }

    @SuppressLint("MissingPermission") // Guarded by hasPostPermission().
    override fun show(signal: AlarmSignal) {
        if (!hasPostPermission()) return

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(signal.message)
            .setContentText(signal.reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(signal.reason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(nextId.incrementAndGet(), notification)
    }

    private fun hasPostPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm triggers",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Shown when an earthquake or test alert fires the alarm"
            }
            appContext.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "alarm_triggers"
    }
}
