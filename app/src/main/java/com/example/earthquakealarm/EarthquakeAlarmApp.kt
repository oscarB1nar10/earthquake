package com.example.earthquakealarm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. @HiltAndroidApp generates and hosts the app-level
 * dependency graph (see [di.AppModule][com.example.earthquakealarm.di.AppModule]);
 * services, the activity, and ViewModels receive their dependencies via
 * @AndroidEntryPoint / @HiltViewModel injection.
 *
 * Registered via `android:name` in the manifest.
 */
@HiltAndroidApp
class EarthquakeAlarmApp : Application()
