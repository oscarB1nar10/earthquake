package com.example.earthquakealarm.domain

import com.example.earthquakealarm.data.AlarmSettings

/**
 * While enabled, fires the alarm on ANY WhatsApp notification, carrying the
 * user's configured message. Matches by package (WhatsApp / WhatsApp Business),
 * so any incoming message triggers it — both a real trigger and a convenient way
 * to test the whole pipeline without waiting for an earthquake.
 */
class WhatsAppRule : NotificationRule {

    override fun evaluate(
        notification: ReceivedNotification,
        settings: AlarmSettings,
    ): AlarmSignal? {
        if (!settings.whatsAppFireEnabled) return null
        if (notification.packageName !in WHATSAPP_PACKAGES) return null
        return AlarmSignal(
            reason = "WhatsApp message (${notification.packageName})",
            message = settings.whatsAppMessage,
        )
    }

    private companion object {
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
    }
}
