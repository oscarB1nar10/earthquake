package com.example.earthquakealarm.domain

/**
 * Posts a local heads-up notification when the alarm fires. Abstracted so the
 * [AlarmDispatcher] depends on the behaviour, not on Android's notification APIs
 * (the implementation lives in the platform layer).
 */
interface AlarmNotifier {
    fun show(signal: AlarmSignal)
}
