
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include "DHT.h"
#define RELAY1  5  
// Update these with values suitable for your network.
//////////////////////////////////////////////////
#define DHTPIN 4 //DHT OUTPUT PIN NUMBER: The output pin of DHT sensor will be connected to this pin
#define DHTTYPE DHT11  //This define the type of DHT sensor
///////////////////////////////
byte sensorInterrupt = 13;  // 0 = digital pin 2
byte sensorPin       = 2;
// The hall-effect flow sensor outputs approximately 7.5 pulses per second per
// litre/minute of flow.
float calibrationFactor = 7.5;
volatile byte pulseCount;  
float flowRate;
unsigned int flowMilliLitres;
unsigned long totalMilliLitres;
unsigned long oldTime;
int totalLitres;
///////////////////////////////
DHT dht(DHTPIN, DHTTYPE); //Creating the object of DHT class
/////////////////////////////////
const char* ssid = "RicktamNw";
const char* password = "tom_116164";
//const char* ssid = "RickMoto";
//const char* password = "tom116164";
const char* mqtt_server = "iot.eclipse.org";
//char* receivedPayload;
int flag1=0,flag2=1;
int receivedMsg=0;

WiFiClient espClient;
PubSubClient client(espClient);
long lastMsg = 0;
char msgMoisture[75],msgMotor[75],waterCap[75],msgHumidity[75],msgTemp[75];
int value = 0;

int rainPin = A0;

void setup_wifi() {

  delay(10);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
 WiFi.mode(WIFI_STA);
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
  
  
  //////////////////////////////////////////////////////////////
  
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
   // receivedPayload[i]=((char)payload[i]);
    Serial.print((char)payload[i]);
    //receivedMsg=((int)((char)payload[i]));
  }
  Serial.print("-------->");
//Serial.println(receivedPayload);
//delay(5000);
//flag=1;

if ((char)payload[0] == 'U') {
  flag2=-1;
}

else if ((char)payload[0] == 'A') {
  flag2=1;
  flag1=0;
}
else if ((char)payload[0] == '1' && flag2==-1) {
    //digitalWrite(BUILTIN_LED, LOW);   // Turn the LED on (Note that LOW is the voltage level
    flag1=1;
   
  } else if ((char)payload[0] == '0'&& flag2==-1) {
    //digitalWrite(BUILTIN_LED, HIGH);  // Turn the LED off by making the voltage HIGH
    flag1=-1;
  }

 

}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ESP8266Client_0789";
    //clientId += String(random(0xffff), HEX);
    // Attempt to connect
    //if (client.connect(clientId.c_str())) {
      if (client.connect(clientId.c_str())) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      //client.publish("outTopic", "hello world");
      // ... and resubscribe
      client.subscribe("Control/Motor",1);
      client.subscribe("Control/Motor/Mode",1);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(1000);
    }
  }
}

void setup() {
   pinMode(rainPin, INPUT);
  pinMode(BUILTIN_LED, OUTPUT);     // Initialize the BUILTIN_LED pin as an output
    pinMode(RELAY1, OUTPUT);   
  Serial.begin(115200);
  setup_wifi();
  dht.begin();
  client.setServer(mqtt_server, 1883);
  //client.subscribe("Control/Motor",1);
  //client.subscribe("Control/Motor/Mode",1);
  client.setCallback(callback);
  
}

void loop() {

  if (!client.connected()) {
     Serial.println("Going to reconnect");
    reconnect();
  }
 // if(client.loop()){
   // Serial.println("Going to reconnect");
  //  reconnect();
  //}

 int sensorValue = analogRead(rainPin);
 //long now = millis();
 // if (now - lastMsg > 2000) {
   // lastMsg = now;
    //++value;
 client.loop();
   if(flag2==1){

        if(sensorValue>250)
      {
          snprintf (msgMoisture, 75, "Moisture Level is #%ld (Need Water) ", sensorValue);
          snprintf (msgMotor, 75, "Motor is running ");
          Serial.println("Publish message <250: ");
         // Serial.println(msg);
          client.publish("Sensor/Moisture", msgMoisture);
          client.publish("Sensor/Motor", msgMotor);
          digitalWrite(RELAY1,0);           // Turns ON Relays 1
         // waterFlowMeter();
          delay(2000);
      }
      else{
         snprintf (msgMoisture, 75, "Moisture Level is #%ld (Not Need Water) ", sensorValue);
          snprintf (msgMotor, 75, "Motor is stopped ");
          Serial.println("Publish message >250: ");
         // Serial.println(msg);
          client.publish("Sensor/Moisture", msgMoisture);
          client.publish("Sensor/Motor", msgMotor);
          digitalWrite(RELAY1,1);           // Turns ON Relays 1
          delay(2000);
      }
    
   }
   if(flag1==1){
    Serial.println("Motor Running");
      digitalWrite(RELAY1,0);           // Turns ON Relays 1
     // waterFlowMeter();
      flag1=0;
   }
   else if(flag1==-1){
    Serial.println("Motor Stopped");
      digitalWrite(RELAY1,1);           // Turns OFF Relays 1
      flag1=0;
   }
   delay(500);
   getTemp();
}
void getTemp(){
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  if(isnan(t) || isnan(h)){
    Serial.println("Failed to read DHT11");
  }else{
   strcpy(msgTemp, "Temperature Value is: ");
    dtostrf(t, 2, 2, &msgTemp[strlen(msgTemp)]);
    // dtostrf(t,2,2,msgTemp);
    strcpy(msgHumidity, "Humidity Value is: ");
    dtostrf(h, 2, 2, &msgHumidity[strlen(msgHumidity)]);
    
    client.publish("Sensor/Temperature", msgTemp);
    client.publish("Sensor/Humidity", msgHumidity);
    Serial.print("Humidity: ");
    Serial.print(msgHumidity);
    Serial.print(" %\t");
    Serial.print("Temperature: ");
   
    Serial.print(msgTemp);
    Serial.println(" *C");
  }
  
}

