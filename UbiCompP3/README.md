# Projekat 3 - Android IoT Control Dashboard

## Opis projekta
Ovaj projekat predstavlja mobilnu Android aplikaciju razvijenu kao prosirenje IoT sistema realizovanog u Projektu 2. Aplikacija ima ulogu kontrolnog dashboard-a i omogucava prikaz senzorskih podataka, prikaz dogadjaja detektovanih ML/DL modelom, prijem notifikacija u realnom vremenu i slanje komandi ka sistemu.

Android aplikacija komunicira sa Raspberry Pi uredjajem preko WebSocket protokola. Raspberry Pi backend servis preuzima podatke sa MQTT topic-a, prosledjuje ih aplikaciji i prima komande iz aplikacije.

## Glavne funkcionalnosti
- prikaz live senzorskih podataka:
  - ambient light
  - RGB vrednosti
  - dominantna boja
  - stanje osvetljenja
- prikaz dogadjaja detektovanih od strane TensorFlow Lite modela
- prikaz actuator dogadjaja
- lokalne Android notifikacije za bitne evente
- konfiguracija parametara sistema:
  - light threshold na SBC strani
  - monitoring enabled / disabled na MCU strani
- pokretanje simulirane akcije aktuatora iz mobilne aplikacije

## Arhitektura sistema
Sistem se sastoji iz sledecih celina:
- Arduino Nano 33 BLE Sense Lite
- Raspberry Pi 4 sa servisima iz Projekta 2
- mobile-dashboard-api servis na Raspberry Pi uredjaju
- Android aplikacija

Tok rada:
1. Arduino meri podatke o osvetljenju i boji
2. Raspberry Pi backend preuzima podatke iz MQTT sistema
3. mobile-dashboard-api servis prosledjuje podatke Android aplikaciji preko WebSocket veze
4. Android aplikacija prikazuje podatke i evente u realnom vremenu
5. Android aplikacija moze da posalje komande nazad ka sistemu

## Koriscene tehnologije
- Kotlin
- Jetpack Compose
- MVVM
- OkHttp WebSocket
- Android Notifications
- Raspberry Pi backend servis
- MQTT
- TensorFlow Lite

## Ekrani aplikacije
### Dashboard
Prikaz trenutnih senzorskih vrednosti, dominantne boje, confidence vrednosti i actuator stanja.

### Events
Prikaz liste detektovanih dogadjaja i reakcija sistema.

### Control
Slanje komandi sistemu:
- enable monitoring
- disable monitoring
- simulate actuator action

### Settings
Podesavanje:
- WebSocket URL
- light threshold

## WebSocket komunikacija
Aplikacija koristi WebSocket konekciju ka Raspberry Pi backend servisu.

Primer URL-a:
```text
ws://RPI_IP_ADRESA:8765