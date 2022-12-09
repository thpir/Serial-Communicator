int led = 12;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  pinMode(12, OUTPUT);
}

void loop() {
  char incomingByte;
  // put your main code here, to run repeatedly:
  if(Serial.available()) {
    incomingByte = Serial.read();
    if(incomingByte == 'o') {
      digitalWrite(led, HIGH);
      Serial.write('o');
    }else if (incomingByte == 'x') {
      digitalWrite(led, LOW);
      Serial.write('x');
    }else {
      // Do nothing
    }
  }
}
