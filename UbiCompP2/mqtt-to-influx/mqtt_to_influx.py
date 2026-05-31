import json
import paho.mqtt.client as mqtt
from influxdb_client import InfluxDBClient, Point, WritePrecision

MQTT_BROKER = "mosquitto"
MQTT_PORT = 1883
MQTT_TOPIC = "iot/sensors/light_color"

INFLUX_URL = "http://influxdb:8086"
INFLUX_TOKEN = "YOUR_INFLUXDB_TOKEN"
INFLUX_ORG = "ubicomp"
INFLUX_BUCKET = "light_color_data"

influx_client = InfluxDBClient(
    url=INFLUX_URL,
    token=INFLUX_TOKEN,
    org=INFLUX_ORG
)
write_api = influx_client.write_api()

def on_connect(client, userdata, flags, rc):
    print(f"Connected to MQTT broker with code {rc}")
    client.subscribe(MQTT_TOPIC)

def on_message(client, userdata, msg):
    try:
        payload = msg.payload.decode("utf-8")
        print(f"Received MQTT: {payload}")

        data = json.loads(payload)

        if "ambient" not in data or "r" not in data or "g" not in data or "b" not in data:
            print("Skipping non-sensor event message.")
            return
    
        point = (
            Point("light_color_measurements")
            .field("active", int(data.get("active", 0)))
            .field("ambient", int(data.get("ambient", 0)))
            .field("r", int(data.get("r", 0)))
            .field("g", int(data.get("g", 0)))
            .field("b", int(data.get("b", 0)))
            .tag("dominant_color", str(data.get("dominant_color", "unknown")))
            .tag("light_state", str(data.get("light_state", "unknown")))
        )

        write_api.write(bucket=INFLUX_BUCKET, org=INFLUX_ORG, record=point)
        print("Written to InfluxDB")
        print("-" * 50)

    except Exception as e:
        print(f"Error processing message: {e}")

def main():
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.loop_forever()

if __name__ == "__main__":
    main()
