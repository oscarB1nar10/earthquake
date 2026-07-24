/*
 * Earthquake Alarm — microcontroller receiver (ESP32, WiFi only)
 * --------------------------------------------------------------
 * Matches the Android app's Esp32AlarmClient. HTTP endpoints:
 *   GET/POST  http://<esp-ip>/alarm[?msg=...]   → sound the alarm
 *   GET       http://<esp-ip>/off               → silence it
 *   GET       http://<esp-ip>/status            → "ON <secs left>" or "OFF"
 *
 * On trigger it drives ALARM_PIN HIGH for up to ALARM_MS (wire it to a relay,
 * buzzer, or siren). Timing is non-blocking so HTTP keeps working while the
 * alarm sounds. Works on any ESP32 variant (WROOM, S2, C3, ...).
 *
 * Arduino IDE: Tools -> Board -> "ESP32 Dev Module", install the
 * "esp32 by Espressif Systems" boards package first.
 */

#include <WiFi.h>
#include <WebServer.h>

// ---------- CONFIG: edit these ----------
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASS = "YOUR_WIFI_PASSWORD";

const int   ALARM_PIN = 4;                    // GPIO4; swap for your relay/buzzer pin
const unsigned long ALARM_MS = 30000UL;       // alarm stays on for up to 30 s
// ----------------------------------------

WebServer     server(80);
unsigned long alarmUntil = 0;

void startAlarm(const char* source) {
  alarmUntil = millis() + ALARM_MS;
  digitalWrite(ALARM_PIN, HIGH);
  Serial.printf("[ALARM] ON via %s\n", source);
}

void stopAlarm(const char* source) {
  alarmUntil = 0;
  digitalWrite(ALARM_PIN, LOW);
  Serial.printf("[ALARM] OFF via %s\n", source);
}

bool alarmIsOn() {
  return alarmUntil != 0;
}

// --- HTTP handlers ---
void handleAlarm() {
  if (server.hasArg("msg")) {
    Serial.printf("[WiFi] message: %s\n", server.arg("msg").c_str());
  }
  startAlarm("WiFi");
  server.send(200, "text/plain", "OK\n");
}

void handleOff() {
  stopAlarm("WiFi");
  server.send(200, "text/plain", "OFF\n");
}

void handleStatus() {
  if (alarmIsOn()) {
    unsigned long secsLeft = (alarmUntil - millis()) / 1000UL;
    server.send(200, "text/plain", "ON " + String(secsLeft) + "\n");
  } else {
    server.send(200, "text/plain", "OFF\n");
  }
}

void handleRoot() {
  server.send(200, "text/plain",
              "ESP32 alarm online. Endpoints: /alarm, /off, /status\n");
}

void setup() {
  Serial.begin(115200);
  pinMode(ALARM_PIN, OUTPUT);
  digitalWrite(ALARM_PIN, LOW);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(400);
    Serial.print(".");
  }
  String ip = WiFi.localIP().toString();
  Serial.printf("\nConnected. Set the app's ESP32 base URL to: http://%s\n", ip.c_str());

  server.on("/", handleRoot);
  server.on("/alarm", HTTP_GET, handleAlarm);
  server.on("/alarm", HTTP_POST, handleAlarm);
  server.on("/off", HTTP_GET, handleOff);
  server.on("/status", HTTP_GET, handleStatus);
  server.begin();
}

void loop() {
  server.handleClient();

  // Auto turn-off after ALARM_MS.
  if (alarmIsOn() && (long)(millis() - alarmUntil) >= 0) {
    stopAlarm("timeout");
  }
}
