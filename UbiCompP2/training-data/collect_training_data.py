import json
import csv
import os
import sys
import paho.mqtt.client as mqtt

MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_TOPIC = "iot/sensors/light_color"

if len(sys.argv) < 2:
    print("Usage: python3 collect_training_data.py <label>")
    print("Example: python3 collect_training_data.py red")
    sys.exit(1)

LABEL = sys.argv[1]
CSV_FILE = "color_training_data.csv"

file_exists = os.path.isfile(CSV_FILE)

csv_file = open(CSV_FILE, mode="a", newline="")
writer = csv.writer(csv_file)

if not file_exists:
    writer.writerow(["ambient", "r", "g", "b", "label"])

print(f"Collecting training data for label: {LABEL}")
print("Press Ctrl+C to stop.\n")

def on_connect(client, userdata, flags, rc):
    print(f"Connected to MQTT broker with code {rc}")
    client.subscribe(MQTT_TOPIC)

def on_message(client, userdata, msg):
    try:
        payload = msg.payload.decode("utf-8")
        data = json.loads(payload)

        ambient = int(data.get("ambient", 0))
        r = int(data.get("r", 0))
        g = int(data.get("g", 0))
        b = int(data.get("b", 0))

        writer.writerow([ambient, r, g, b, LABEL])
        csv_file.flush()

        print(f"Saved: ambient={ambient}, r={r}, g={g}, b={b}, label={LABEL}")

    except Exception as e:
        print(f"Error processing message: {e}")

def main():
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.loop_forever()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nStopping data collection.")
        csv_file.close()
