package com.example.earthquakealarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * All the things the UI can ask the ViewModel to do, bundled so the stateless
 * [MainScreen] keeps a tidy signature and stays easy to preview/test.
 */
data class MainScreenActions(
    val onKeywordsChange: (String) -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onWifiEnabledChange: (Boolean) -> Unit,
    val onEsp32BaseUrlChange: (String) -> Unit,
    val onFireAlarm: () -> Unit,
    val onStopAlarm: () -> Unit,
    val onCheckStatus: () -> Unit,
    val onWhatsAppFireEnabledChange: (Boolean) -> Unit,
    val onWhatsAppMessageChange: (String) -> Unit,
    val onTestTrigger: () -> Unit,
    val onClearLog: () -> Unit,
)

/**
 * Stateful entry point: connects the Hilt-provided [MainViewModel] to the
 * stateless screen, owns the notification-permission launcher, and refreshes
 * system state on resume.
 */
@Composable
fun MainRoute(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-check notification access + screen watcher whenever we return here.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Android 13+ needs runtime permission for the app to post its own notifications.
    val postNotificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result not needed; the notifier checks the permission before posting */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MainScreen(
        state = state,
        actions = MainScreenActions(
            onKeywordsChange = viewModel::onKeywordsChange,
            onOpenNotificationSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            onOpenAccessibilitySettings = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onWifiEnabledChange = viewModel::onWifiEnabledChange,
            onEsp32BaseUrlChange = viewModel::onEsp32BaseUrlChange,
            onFireAlarm = viewModel::onFireAlarm,
            onStopAlarm = viewModel::onStopAlarm,
            onCheckStatus = viewModel::onCheckStatus,
            onWhatsAppFireEnabledChange = viewModel::onWhatsAppFireEnabledChange,
            onWhatsAppMessageChange = viewModel::onWhatsAppMessageChange,
            onTestTrigger = viewModel::onTestTrigger,
            onClearLog = viewModel::onClearLog,
        ),
    )
}

/** Pure UI: renders [state] and emits [actions]. No Android/ViewModel knowledge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(state: MainUiState, actions: MainScreenActions) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Scaffold(topBar = { TopAppBar(title = { Text("Earthquake → Alarm") }) }) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1) Detection access — the two ways alerts are caught
            SectionCard("1 · Detection access") {
                // Path A: notifications (Be Aware alerts, WhatsApp, other quake apps)
                StatusRow(
                    ok = state.notificationAccessGranted,
                    okText = "Notifications — intercepting",
                    badText = "Notifications — access not granted",
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = actions.onOpenNotificationSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.notificationAccessGranted) "Open notification access settings"
                        else "Grant notification access"
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Path B: screen watcher (full-screen "Take Action" alerts)
                StatusRow(
                    ok = state.screenWatcherEnabled,
                    okText = "Screen watcher — catching full-screen alerts",
                    badText = "Screen watcher — off (full-screen alerts may be missed)",
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = actions.onOpenAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.screenWatcherEnabled) "Open accessibility settings"
                        else "Enable screen watcher (accessibility)"
                    )
                }
                CardHint(
                    "Google's strongest 'Take Action' earthquake alert is a full-screen alarm, " +
                        "not always a notification. The screen watcher reads alert screens from " +
                        "Google Play services / emergency broadcasts and fires when their text " +
                        "matches the keywords."
                )
            }

            // 2) Detection keywords
            SectionCard("2 · Detection keywords") {
                CardHint("A notification triggers the alarm if its text contains any of these (comma-separated, case-insensitive).")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.settings.keywords,
                    onValueChange = actions.onKeywordsChange,
                    label = { Text("Keywords") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 3) ESP32 alarm (WiFi / HTTP)
            SectionCard("3 · ESP32 alarm (WiFi/HTTP)") {
                ToggleRow(
                    "Auto-fire on earthquake / WhatsApp",
                    state.settings.wifiEnabled,
                    actions.onWifiEnabledChange,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.settings.esp32BaseUrl,
                    onValueChange = actions.onEsp32BaseUrlChange,
                    label = { Text("ESP32 base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CardHint("e.g. http://192.168.1.7 — the app calls /alarm, /off, /status on this host. Phone must be on the same WiFi.")
                Spacer(Modifier.height(12.dp))
                Text("Manual control", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = actions.onFireAlarm, modifier = Modifier.weight(1f)) {
                        Text("Fire alarm")
                    }
                    Button(
                        onClick = actions.onStopAlarm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Turn off alarm")
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = actions.onCheckStatus,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Check status") }
            }

            // 4) Fire alarm on WhatsApp
            SectionCard("4 · Fire alarm on WhatsApp message") {
                ToggleRow(
                    "Fire the alarm on any WhatsApp message",
                    state.settings.whatsAppFireEnabled,
                    actions.onWhatsAppFireEnabledChange,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.settings.whatsAppMessage,
                    onValueChange = actions.onWhatsAppMessageChange,
                    label = { Text("Alarm message (sent to device & notification)") },
                    enabled = state.settings.whatsAppFireEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                CardHint("While on, ANY WhatsApp message fires the alarm. Needs the ESP32 (WiFi) alarm above enabled to reach the device. Turn off when you're done so normal chats don't trip it.")
            }

            // 5) Test
            SectionCard("5 · Test") {
                CardHint("Fires every enabled transport right now, exactly as a real alert would.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = actions.onTestTrigger, modifier = Modifier.fillMaxWidth()) {
                    Text("Simulate earthquake (test trigger)")
                }
            }

            // 6) Event log
            SectionCard("6 · Event log") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardHint("${state.events.size} recent events")
                    OutlinedButton(onClick = actions.onClearLog) { Text("Clear") }
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                if (state.events.isEmpty()) {
                    CardHint("No events yet. Grant access above and tap the test button.")
                } else {
                    state.events.forEach { entry ->
                        Text(
                            text = "${timeFormat.format(Date(entry.timestampMillis))}  ${entry.message}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
