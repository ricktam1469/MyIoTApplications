
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#define RELAY1  5  
// Update these with values suitable for your network.

const char* ssid = "RicktamNw";
const char* password = "tom_116164";
const char* mqtt_server = "iot.eclipse.org";

WiFiClient espClient;
PubSubClient client(espClient);
long lastMsg = 0;
char msgMoisture[60],msgMotor[60];
int value = 0;

int rainPin = A0;

void setup_wifi() {

  delay(10);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();

  // Switch on the LED if an 1 was received as first character
  if ((char)payload[0] == '1') {
    digitalWrite(BUILTIN_LED, LOW);   // Turn the LED on (Note that LOW is the voltage level
    // but actually the LED is on; this is because
    // it is acive low on the ESP-01)
  } else {
    digitalWrite(BUILTIN_LED, HIGH);  // Turn the LED off by making the voltage HIGH
  }

}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (client.connect(clientId.c_str())) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      //client.publish("outTopic", "hello world");
      // ... and resubscribe
      client.subscribe("inTopic");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void setup() {
   pinMode(rainPin, INPUT);
  pinMode(BUILTIN_LED, OUTPUT);     // Initialize the BUILTIN_LED pin as an output
    pinMode(RELAY1, OUTPUT);   
  Serial.begin(115200);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void loop() {

  if (!client.connected()) {
    reconnect();
  }
  client.loop();
 int sensorValue = analogRead(rainPin);
 //long now = millis();
 // if (now - lastMsg > 2000) {
   // lastMsg = now;
    //++value;
    if(sensorValue>250)
    {
        snprintf (msgMoisture, 75, "Moisture Level is #%ld (Need Water) ", sensorValue);
        snprintf (msgMotor, 75, "Motor is running ", sensorValue);
        Serial.print("Publish message: ");
       // Serial.println(msg);
        client.publish("Sensor/Moisture", msgMoisture);
        client.publish("Sensor/Motor", msgMotor);
        digitalWrite(RELAY1,0);           // Turns ON Relays 1

    }
    //snprintf (msg, 75, "Voltage level of Moisture is #%ld", sensorValue);
    //Serial.print("Publish message: ");
   // Serial.println(msg);
   // client.publish("outTopic", msg);
 // }
}
