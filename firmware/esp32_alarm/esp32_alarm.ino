#include <WiFi.h>
// This library lets the ESP32 create a simple web server.
#include <WebServer.h>

const char* wifi_net = "J.J";
const char* wifi_pass = "MovB1nar10.01";

// This creates a web server on port 80.
// Port 80 is the normal port used by websites with http://
WebServer server(80);

// GPIO26 means pin number 26 on the ESP32
const int ALARM_PIN = 4;
// Turn alarm ON
const int ALARM_ON = LOW;
// Turn alarm OFF
const int ALARM_OFF = HIGH;
bool isAlarmActive = false;

// This variable stores the time when the alarm should stop.
unsigned long alarmEndTime = 0;

// This function turns the alarm ON for a specific amout of time in milliseconds
void turnAlarmOn(unsigned long durationMs) {
  // Send ON signal to the alarm pin.
  digitalWrite(ALARM_PIN, ALARM_ON);
  isAlarmActive = true;
  // Calculate when the alarm should turn off.
  // millis() gives the time since the ESP32 started running.
  alarmEndTime = millis() + durationMs;
}

void turnAlarmOff() {
  digitalWrite(ALARM_PIN, ALARM_OFF);
  isAlarmActive = false;
}

void handleAlarm() {
  // Turn the alarm ON for 10 seconds
  turnAlarmOn(10000);
  // Send a response back to the phone
  server.send(200, "text/plain", "Alarm ON for 10 seconds");
}

// This function runs when someone opens /off
void handleOff() {
  turnAlarmOff();
  server.send(200, "text/plain", "Alarm OFF");
}

void handleAlarmStatus() {
  if(isAlarmActive) {
    server.send(200, "text/plain", "Alarm is active");
  } else {
    server.send(200, "text/plain", "Alarm is off");
  }
}

// setup() runs only one time when the ESP32 turns on or resets
void setup() {
  // Start serial communication between ESP32 and computer
  // 115200 is the communication speed.
  Serial.begin(115200);
  delay(2000);
  // Configure ALARM_PIN as an output pin.
  // Output means the ESP32 will send voltage from this pin.
  pinMode(ALARM_PIN, OUTPUT);
  // Start with the alarm turned OFF.
  digitalWrite(ALARM_PIN, ALARM_OFF);
  // Start connecting the ESP32 to our wi-fi network
  WiFi.begin(wifi_net, wifi_pass);

  // Print message
  Serial.print("Connnecting to wifi");

  // This loop waits until the ESP32 connects to wi-fi
  while (WiFi.status() != WL_CONNECTED) {
    // Wait half a second
    delay(500);
    Serial.print(".");
  }

  // Print a blank line after connecting
  Serial.println();
  // Print a confirmation message
  Serial.println("Connected!");
  Serial.print("ESP32 IP address: ");
  // Print the IP address assigned to the ESP32
  Serial.println(WiFi.localIP());

  // Create the /alarm route using POST
  // Example from browser:
  // http://ESP32_IP/alarm
  server.on("/alarm", HTTP_GET, handleAlarm);
  server.on("/alarm", HTTP_POST, handleAlarm);
  server.on("/off", HTTP_GET, handleOff);
  server.on("/status", HTTP_GET, handleAlarmStatus);

  // Start the wen server
  server.begin();
}

void loop() {
  // This checks if someone is trying to access /alar, /off, or /status
  server.handleClient();
  // Check if the alarm is active AND if the current time passed alarmEndTime
  if (isAlarmActive && millis() > alarmEndTime) {
    turnAlarmOff();
  }
}