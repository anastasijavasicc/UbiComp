# Projekat 2 - Voice-activated light and color monitoring system

## Opis projekta
Ovaj projekat predstavlja prosirenje IoT sistema razvijenog u prethodnom projektu. Nakon aktiviranja glasovnom komandom, Arduino Nano 33 BLE Sense Lite prikuplja podatke o osvetljenju i boji pomocu APDS9960 senzora i salje ih preko serijske veze na Raspberry Pi 4.

Na Raspberry Pi uredjaju podaci se prosledjuju na MQTT broker, cuvaju u InfluxDB bazi, vizuelizuju u Grafani i analiziraju pomocu TensorFlow Lite modela za klasifikaciju dominantne boje. Pored toga, implementirana je i detekcija promene osvetljenja, uz simuliranu akciju aktuatora.

## Korisceni hardver
- Arduino Nano 33 BLE Sense Lite
- Raspberry Pi 4 Model B
- APDS9960 senzor (ambient light + RGB color)

## Koriscene tehnologije
- Arduino IDE
- Python
- MQTT (Mosquitto)
- InfluxDB
- Grafana
- Docker / Docker Compose
- TensorFlow Lite

## Arhitektura sistema
1. Arduino ocitava podatke: ambient, R, G, B, dominant_color, light_state, active
2. Podaci se preko serial veze salju na Raspberry Pi
3. `serial-reader` servis cita podatke i publishuje ih na MQTT topic
4. `mqtt-to-influx` servis upisuje podatke u InfluxDB
5. Grafana prikazuje dashboard sa vrednostima osvetljenja i RGB kanala
6. `tflite-analyzer` servis koristi TensorFlow Lite model za klasifikaciju dominantne boje
7. U slucaju promene osvetljenja, pokrece se simulirana akcija aktuatora

## MQTT topic-i
- `iot/sensors/light_color`
- `iot/events/detection`
- `iot/actuator/action`

## Pokretanje projekta
U root folderu projekta pokrenuti:

```bash
docker compose up -d --build
