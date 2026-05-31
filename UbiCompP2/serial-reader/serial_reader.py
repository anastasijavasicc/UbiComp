import serial
import json
import time
import paho.mqtt.client as mqtt

SERIAL_PORT = "/dev/ttyACM0"
BAUD_RATE = 115200

MQTT_BROKER = "mosquitto"
MQTT_PORT = 1883
MQTT_TOPIC = "iot/sensors/light_color"

def main():
    print(f"Opening serial port {SERIAL_PORT} at {BAUD_RATE} baud...")
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
    time.sleep(2)

    print(f"Connecting to MQTT broker at {MQTT_BROKER}:{MQTT_PORT}...")
    client = mqtt.Client()
    client.connect(MQTT_BROKER, MQTT_PORT, 60)

    print("Reading serial data and publishing to MQTT...\n")

    while True:
        try:
            line = ser.readline().decode("utf-8", errors="ignore").strip()
            if not line:
                continue

            print(f"RAW: {line}")

            try:
                data = json.loads(line)
                payload = json.dumps(data)

                client.publish(MQTT_TOPIC, payload)

                print("PUBLISHED TO MQTT:")
                print(payload)
                print("-" * 50)

            except json.JSONDecodeError:
                print("Not valid JSON, skipping.")
                print("-" * 50)

        except KeyboardInterrupt:
            print("\nStopping serial reader.")
            break
        except Exception as e:
            print(f"Error: {e}")
            time.sleep(1)

if __name__ == "__main__":
    main()
import serial
import json
import time
import paho.mqtt.client as mqtt

SERIAL_PORT = "/dev/ttyACM0"
BAUD_RATE = 115200

MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_TOPIC = "iot/sensors/light_color"

def main():
    print(f"Opening serial port {SERIAL_PORT} at {BAUD_RATE} baud...")
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
    time.sleep(2)

    print(f"Connecting to MQTT broker at {MQTT_BROKER}:{MQTT_PORT}...")
    client = mqtt.Client()
    client.connect(MQTT_BROKER, MQTT_PORT, 60)

    print("Reading serial data and publishing to MQTT...\n")

    while True:
        try:
            line = ser.readline().decode("utf-8", errors="ignore").strip()
            if not line:
                continue

            print(f"RAW: {line}")

            try:
                data = json.loads(line)
                payload = json.dumps(data)

                client.publish(MQTT_TOPIC, payload)

                print("PUBLISHED TO MQTT:")
                print(payload)
                print("-" * 50)

            except json.JSONDecodeError:
                print("Not valid JSON, skipping.")
                print("-" * 50)

        except KeyboardInterrupt:
            print("\nStopping serial reader.")
            break
        except Exception as e:
            print(f"Error: {e}")
            time.sleep(1)

if __name__ == "__main__":
    main()
