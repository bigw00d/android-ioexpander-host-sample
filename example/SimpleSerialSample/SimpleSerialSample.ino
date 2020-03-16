void setup() {
  Serial.begin(115200);
}

// the loop routine runs over and over again forever:
void loop() {
  Serial.println("01234");
  // Serial.write(sendData[sentCnt]);
  delay(10000);        // delay in between reads for stability
}
