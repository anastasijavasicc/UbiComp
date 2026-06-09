import serial
import json
import time
import paho.mqtt.client as mqtt

SERIAL_PORT = "/dev/ttyACM0"
BAUD_RATE = 115200

MQTT_BROKER = "mosquitto"
MQTT_PORT = 1883
MQTT_SENSOR_TOPIC = "iot/sensors/light_color"
MQTT_MCU_COMMAND_TOPIC = "iot/app/mcu_commands"

ser = None


def send_command_to_arduino(command: str):
    global ser

    if ser is None:
        print("Serial port is not initialized.")
        return

    try:
        ser.write((command + "\n").encode("utf-8"))
        ser.flush()
        print(f"Sent to Arduino: {command}")
    except Exception as e:
        print(f"Error writing to Arduino: {e}")


def on_connect(client, userdata, flags, rc, properties=None):
    print(f"Connected to MQTT broker with code {rc}")
    client.subscribe(MQTT_MCU_COMMAND_TOPIC)


def on_message(client, userdata, msg):
    try:
        payload = msg.payload.decode("utf-8")
        data = json.loads(payload)

        command = data.get("command", "")

        if command == "set_monitoring":
            enabled = bool(data.get("enabled", False))
            if enabled:
                send_command_to_arduino("MONITORING_ON")
            else:
                send_command_to_arduino("MONITORING_OFF")

        elif command == "simulate_actuator":
            send_command_to_arduino("ACTUATOR_SIMULATE")

    except Exception as e:
        print(f"Error handling MCU command: {e}")


def main():
    global ser

    print(f"Opening serial port {SERIAL_PORT} at {BAUD_RATE} baud...")
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
    time.sleep(2)

    print(f"Connecting to MQTT broker at {MQTT_BROKER}:{MQTT_PORT}...")
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.loop_start()

    print("Reading serial data and publishing to MQTT...\n")

    try:
        while True:
            line = ser.readline().decode("utf-8", errors="ignore").strip()
            if not line:
                continue

            print(f"RAW: {line}")

            try:
                data = json.loads(line)
                payload = json.dumps(data)

                info = client.publish(MQTT_SENSOR_TOPIC, payload)
                info.wait_for_publish()

                print("PUBLISHED TO MQTT:")
                print(payload)
                print("-" * 50)

            except json.JSONDecodeError:
                print("Not valid JSON, skipping.")
                print("-" * 50)

    except KeyboardInterrupt:
        print("\nStopping serial reader.")
    except Exception as e:
        print(f"Error: {e}")
        time.sleep(1)
    finally:
        client.loop_stop()
        client.disconnect()
        if ser is not None:
            ser.close()


if __name__ == "__main__":
    main()
