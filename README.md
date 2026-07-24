# Earthquake Alarm

A minimal one-screen Android app that **detects Google's Android Earthquake Alerts**
(via notifications *and* the full-screen alert) and **fires an ESP32 over WiFi/HTTP**
to activate a physical alarm.

```
 ┌──────────────┐  notification /  ┌─────────────┐    WiFi (HTTP)    ┌──────────────┐
 │  Android      │  full-screen     │  This app    │ ────────────────▶ │  ESP32 +     │
 │  System /     │  alert           │ (listeners + │  /alarm /off      │  relay/buzzer│ ─▶ siren
 │  Play services│ ───────────────▶ │  dispatcher) │  /status          │              │
 └──────────────┘                  └─────────────┘                   └──────────────┘
```

## How it works

- Detection has **two independent paths**, because Google's earthquake system has
  two alert tiers:
  - **Notification listener** (`EarthquakeNotificationListener`): catches *Be Aware*
    alerts and anything else that arrives as a notification (WhatsApp, other quake
    apps). Runs a chain of `NotificationRule`s (first match wins) against a
    configurable, case-insensitive **keyword list**.
  - **Screen watcher** (`EarthquakeScreenWatcherService`, an AccessibilityService):
    catches the strongest *Take Action* alert, which is a **full-screen takeover**
    that may never appear as a notification. It only receives window events from
    Google Play services / Personal Safety / emergency-broadcast packages (filtered
    in `res/xml/accessibility_config.xml`), reads the alert screen's text, and fires
    on a keyword match. Enable it in Settings → Accessibility.
  Both paths produce an `AlarmSignal(reason, message)` into the same dispatcher.
- On a match, `AlarmDispatcher` posts a **local notification** (headline = the
  message) and fires the WiFi transport: HTTP `GET` to
  `{ESP32 base URL}/alarm?msg=<message>` (e.g. `http://192.168.1.7/alarm`).

The ESP32 card also has **manual controls** — **Fire alarm** (`/alarm`), the red
**Turn off alarm** (`/off`), and **Check status** (`/status`) — that call the device
directly, independent of the auto toggle. Plain HTTP requires
`android:usesCleartextTraffic="true"` (already set in the manifest).

## Architecture (MVVM + Hilt + unidirectional data flow)

```
        ┌──────────────────────── UI layer ────────────────────────┐
        │  MainActivity → MainRoute → MainScreen (stateless)        │
        │      renders MainUiState, emits MainScreenActions         │
        │                        ▲   │ intents                      │
        │                 state  │   ▼                              │
        │              MainViewModel (@HiltViewModel)               │
        └───────────────────────┬───────────────────────────────────┘
                                 │ depends on (interfaces)
        ┌──────────── domain layer ───────────┐   ┌──── data layer ────┐
        │ AlarmDispatcher → AlarmNotifier      │   │ SettingsRepository │
        │ AlarmTransport (Strategy)            │   │  └ PrefsSettings…  │
        │   └ WifiTransport → Esp32AlarmClient │   │ EventLogRepository │
        │ NotificationRuleEngine               │   │ AlarmSettings /    │
        │   ├ EarthquakeRule (pure matcher)    │   │ LogEntry (models)  │
        │   └ WhatsAppRule                     │   └────────────────────┘
        └──────────────────────────────────────┘
   EarthquakeNotificationListener ─┐ (@AndroidEntryPoint services)
   EarthquakeScreenWatcherService ─┴──► AlarmDispatcher
   di/AppModule (Hilt, SingletonComponent) provides the whole graph.
```

```
app/src/main/java/com/example/earthquakealarm/
  EarthquakeAlarmApp.kt            @HiltAndroidApp application
  di/AppModule.kt                  Hilt module — the one place the graph is wired
  data/                            repositories + immutable models
    AlarmSettings.kt  SettingsRepository.kt  PrefsSettingsRepository.kt
    LogEntry.kt       EventLogRepository.kt
  domain/                          business logic, framework-light
    AlarmTransport.kt (Strategy + TransportResult)  WifiTransport.kt
    Esp32AlarmClient.kt  AlarmDispatcher.kt  AlarmSignal.kt  AlarmNotifier.kt
    NotificationRule.kt  NotificationRuleEngine.kt  EarthquakeRule.kt  WhatsAppRule.kt
    EarthquakeMatcher.kt  NotificationAccessManager.kt  ScreenWatcherAccessManager.kt
  platform/AndroidAlarmNotifier.kt local notification implementation
  service/                         system entry points (@AndroidEntryPoint)
    EarthquakeNotificationListener.kt  EarthquakeScreenWatcherService.kt
  ui/                              MVVM presentation
    MainActivity.kt  MainViewModel.kt  MainUiState.kt
    MainScreen.kt (Route + stateless Screen + Actions)  Components.kt
firmware/esp32_alarm/esp32_alarm.ino          reference receiver (WiFi only)
```

**Patterns used:** MVVM with a single immutable `MainUiState` and unidirectional
data flow; **Hilt** for dependency injection (`AppModule` provides singletons;
services use `@AndroidEntryPoint` field injection, the ViewModel `@HiltViewModel`);
**Repository** for settings/log; **Strategy** for transports and notification rules
(add one and register it in `AppModule`). `EarthquakeMatcher` is deliberately pure
so the detection rule is unit-testable without Android.

## Build & install

Java 17 and the Android SDK (platform 34, build-tools 34) are required.

**From Android Studio:** `File ▸ Open` this folder, let Gradle sync, then Run.

**From the command line (Windows):**
```bat
gradlew.bat assembleDebug
```
The APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Install with:
```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

> `local.properties` was generated pointing at your SDK. It is machine-specific and
> git-ignored.

## Using the app (one screen, top to bottom)

1. **Detection access** – grant **notification access** and enable the **screen
   watcher** (accessibility) so both alert tiers are covered.
2. **Detection keywords** – tweak if needed; matched against notifications *and*
   alert screens.
3. **ESP32 alarm** – set the base URL (e.g. `http://192.168.1.7`); use **Fire /
   Turn off / Status** to control the device manually.
4. **Fire alarm on WhatsApp** – optional: any WhatsApp message fires the alarm.
5. **Test** – full-pipeline simulation.
6. **Event log** – live feed of matches and send results.

## Microcontroller side

Flash `firmware/esp32_alarm/esp32_alarm.ino` to any ESP32. Set your WiFi
credentials at the top; it serves `/alarm`, `/off`, and `/status` over HTTP. Wire
`ALARM_PIN` (GPIO4 by default) to a relay/buzzer. On boot it prints the base URL to
paste into the app. Give the board a static/reserved IP in your router.

## Important caveats (please read)

- **Reliability / life-safety:** this is a hobby project, not a certified safety
  device. Google's earthquake alerts are best-effort and the strongest shaking can
  arrive within seconds. Don't rely on this as your only warning system.
- **Background survival:** Android may kill background services. For real use, exempt
  the app from battery optimization and keep the phone charging. The notification
  listener auto-rebinds, but aggressive OEM battery managers (Xiaomi, Huawei, etc.)
  may need a manual "no restrictions" / "autostart" setting.
- **Same network:** the phone and the ESP32 must be on the same LAN, and the ESP32
  should have a static/reserved IP so the URL stays valid.
- **Fire on WhatsApp:** the **"Fire alarm on WhatsApp message"** card — switch it on
  and any WhatsApp notification fires the alarm with a custom message (default
  `"Mi abogada favorita eres tuuuu"`). Needs the ESP32 (WiFi) alarm enabled to reach
  the device. Turn it off when done so normal chats don't trip the alarm.
- **Turning the alarm off:** the red **Turn off alarm** button calls `/off` directly;
  the firmware also auto-stops after `ALARM_MS` (30 s by default).
- **Testing without a real quake:** the **Fire alarm** button, the full-pipeline
  **Simulate earthquake** button, or the WhatsApp option above. Best end-to-end test:
  enable the screen watcher, then open Android's own earthquake-alert **demo**
  (Settings → Safety & emergency → Earthquake alerts → *See a demo* — "Test
  Terremoto"); its text matches the keywords and fires the whole pipeline.
- **Screen watcher on sideloaded APKs (Android 13+):** if the accessibility toggle
  is greyed out ("Restricted setting"), open App info → ⋮ → **Allow restricted
  settings**, then enable it.
