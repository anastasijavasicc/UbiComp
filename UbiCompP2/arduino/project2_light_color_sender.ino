#include <Arduino_APDS9960.h>

bool active = true;  // za sada drzimo aktivno stalno, kasnije mozemo dodati voice trigger

String getDominantColor(int r, int g, int b) {
  int maxVal = max(r, max(g, b));

  // ako su vrednosti vrlo slicne, boja je neutralna
  if (abs(r - g) < 15 && abs(g - b) < 15 && abs(r - b) < 15) {
    return "neutral";
  }

  if (maxVal == r) return "red";
  if (maxVal == g) return "green";
  if (maxVal == b) return "blue";

  return "neutral";
}

String getLightState(int ambient) {
  if (ambient < 50) return "dark";
  if (ambient < 200) return "normal";
  return "bright";
}

void setup() {
  Serial.begin(115200);
  while (!Serial);

  if (!APDS.begin()) {
    Serial.println("{\"error\":\"Failed to initialize APDS9960\"}");
    while (1);
  }

  Serial.println("{\"status\":\"APDS9960 initialized\"}");
}

void loop() {
  if (APDS.colorAvailable()) {
    int r, g, b, ambient;
    APDS.readColor(r, g, b, ambient);

    String dominantColor = getDominantColor(r, g, b);
    String lightState = getLightState(ambient);

    Serial.print("{");
    Serial.print("\"active\":");
    Serial.print(active ? 1 : 0);
    Serial.print(",\"ambient\":");
    Serial.print(ambient);
    Serial.print(",\"r\":");
    Serial.print(r);
    Serial.print(",\"g\":");
    Serial.print(g);
    Serial.print(",\"b\":");
    Serial.print(b);
    Serial.print(",\"dominant_color\":\"");
    Serial.print(dominantColor);
    Serial.print("\",\"light_state\":\"");
    Serial.print(lightState);
    Serial.println("\"}");

    delay(1000);
  }
}