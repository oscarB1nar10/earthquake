package com.example.earthquakealarm.di

import android.content.Context
import com.example.earthquakealarm.data.EventLogRepository
import com.example.earthquakealarm.data.PrefsSettingsRepository
import com.example.earthquakealarm.data.SettingsRepository
import com.example.earthquakealarm.domain.AlarmDispatcher
import com.example.earthquakealarm.domain.AlarmNotifier
import com.example.earthquakealarm.domain.EarthquakeMatcher
import com.example.earthquakealarm.domain.EarthquakeRule
import com.example.earthquakealarm.domain.Esp32AlarmClient
import com.example.earthquakealarm.domain.NotificationAccessManager
import com.example.earthquakealarm.domain.NotificationRuleEngine
import com.example.earthquakealarm.domain.ScreenWatcherAccessManager
import com.example.earthquakealarm.domain.WhatsAppRule
import com.example.earthquakealarm.domain.WifiTransport
import com.example.earthquakealarm.platform.AndroidAlarmNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module wiring the whole object graph (replaces the old manual
 * AppContainer). Everything is @Singleton and lives for the app's lifetime.
 * The data/domain classes stay free of DI annotations — this module is the one
 * place that knows how they are constructed.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- data layer ---

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        PrefsSettingsRepository(context)

    @Provides
    @Singleton
    fun provideEventLogRepository(): EventLogRepository = EventLogRepository()

    // --- domain layer ---

    @Provides
    @Singleton
    fun provideEarthquakeMatcher(): EarthquakeMatcher = EarthquakeMatcher()

    @Provides
    @Singleton
    fun provideNotificationAccessManager(
        @ApplicationContext context: Context,
    ): NotificationAccessManager = NotificationAccessManager(context)

    @Provides
    @Singleton
    fun provideScreenWatcherAccessManager(
        @ApplicationContext context: Context,
    ): ScreenWatcherAccessManager = ScreenWatcherAccessManager(context)

    @Provides
    @Singleton
    fun provideEsp32AlarmClient(): Esp32AlarmClient = Esp32AlarmClient()

    /** Registered detection rules, evaluated in order (first match wins). */
    @Provides
    @Singleton
    fun provideNotificationRuleEngine(matcher: EarthquakeMatcher): NotificationRuleEngine =
        NotificationRuleEngine(
            listOf(
                EarthquakeRule(matcher),
                WhatsAppRule(),
            )
        )

    @Provides
    @Singleton
    fun provideAlarmNotifier(@ApplicationContext context: Context): AlarmNotifier =
        AndroidAlarmNotifier(context)

    /** Transports are built here; add a new strategy to the list and it just works. */
    @Provides
    @Singleton
    fun provideAlarmDispatcher(
        esp32AlarmClient: Esp32AlarmClient,
        settingsRepository: SettingsRepository,
        eventLog: EventLogRepository,
        notifier: AlarmNotifier,
    ): AlarmDispatcher = AlarmDispatcher(
        transports = listOf(WifiTransport(esp32AlarmClient)),
        settingsRepository = settingsRepository,
        eventLog = eventLog,
        notifier = notifier,
    )
}
