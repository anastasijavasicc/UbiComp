import asyncio
import json
import paho.mqtt.client as mqtt
import websockets

MQTT_BROKER = "mosquitto"
MQTT_PORT = 1883

TOPIC_SENSOR = "iot/sensors/light_color"
TOPIC_DETECTION = "iot/events/detection"
TOPIC_ACTUATOR = "iot/actuator/action"

TOPIC_APP_COMMANDS = "iot/app/commands"
TOPIC_APP_CONFIG = "iot/app/config"
TOPIC_MCU_COMMANDS = "iot/app/mcu_commands"

WS_HOST = "0.0.0.0"
WS_PORT = 8765

connected_clients = set()

latest_state = {
    "sensor_update": None,
    "detection_event": None,
    "actuator_event": None
}

loop = None
mqtt_client = None


async def broadcast_message(message: dict):
    if not connected_clients:
        return

    dead_clients = set()

    for ws in connected_clients:
        try:
            await ws.send(json.dumps(message))
        except Exception:
            dead_clients.add(ws)

    for ws in dead_clients:
        connected_clients.discard(ws)


def on_connect(client, userdata, flags, rc, properties=None):
    print(f"Connected to MQTT broker with code {rc}")
    client.subscribe(TOPIC_SENSOR)
    client.subscribe(TOPIC_DETECTION)
    client.subscribe(TOPIC_ACTUATOR)


def on_message(client, userdata, msg):
    global loop

    try:
        payload = msg.payload.decode("utf-8")
        data = json.loads(payload)

        if msg.topic == TOPIC_SENSOR:
            message = {
                "type": "sensor_update",
                "data": data
            }
            latest_state["sensor_update"] = message

        elif msg.topic == TOPIC_DETECTION:
            message = {
                "type": "detection_event",
                "data": data
            }
            latest_state["detection_event"] = message

        elif msg.topic == TOPIC_ACTUATOR:
            message = {
                "type": "actuator_event",
                "data": data
            }
            latest_state["actuator_event"] = message

        else:
            return

        print(f"MQTT -> WS: {message}")

        if loop is not None:
            asyncio.run_coroutine_threadsafe(
                broadcast_message(message),
                loop
            )

    except Exception as e:
        print(f"Error processing MQTT message: {e}")


async def handle_client(websocket):
    global mqtt_client

    connected_clients.add(websocket)
    print("Android client connected")

    try:
        for key in ["sensor_update", "detection_event", "actuator_event"]:
            if latest_state[key] is not None:
                await websocket.send(json.dumps(latest_state[key]))

        async for message in websocket:
            print(f"Received from Android: {message}")

            try:
                data = json.loads(message)
                msg_type = data.get("type", "")

                if msg_type == "command":
                    action = data.get("action", "")

                    mqtt_client.publish(TOPIC_APP_COMMANDS, json.dumps(data))

                    if action == "simulate_actuator":
                        mcu_message = {
                            "command": "simulate_actuator"
                        }
                        mqtt_client.publish(TOPIC_MCU_COMMANDS, json.dumps(mcu_message))

                        actuator_message = {
                            "action": "simulated_alert",
                            "reason": "manual_trigger_from_mobile_app"
                        }
                        mqtt_client.publish(TOPIC_ACTUATOR, json.dumps(actuator_message))

                    elif action == "set_monitoring":
                        enabled = bool(data.get("enabled", False))
                        mcu_message = {
                            "command": "set_monitoring",
                            "enabled": enabled
                        }
                        mqtt_client.publish(TOPIC_MCU_COMMANDS, json.dumps(mcu_message))

                    response = {
                        "type": "ack",
                        "message": "Command forwarded to backend",
                        "data": data
                    }
                    await websocket.send(json.dumps(response))

                elif msg_type == "config_update":
                    mqtt_client.publish(TOPIC_APP_CONFIG, json.dumps(data))

                    response = {
                        "type": "ack",
                        "message": "Configuration forwarded to backend",
                        "data": data
                    }
                    await websocket.send(json.dumps(response))

                else:
                    response = {
                        "type": "ack",
                        "message": "Message received",
                        "data": data
                    }
                    await websocket.send(json.dumps(response))

            except Exception as e:
                error_msg = {
                    "type": "error",
                    "message": str(e)
                }
                await websocket.send(json.dumps(error_msg))

    finally:
        connected_clients.discard(websocket)
        print("Android client disconnected")


async def main():
    global loop, mqtt_client
    loop = asyncio.get_running_loop()

    mqtt_client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message
    mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
    mqtt_client.loop_start()

    print(f"Starting WebSocket server on ws://{WS_HOST}:{WS_PORT}")
    async with websockets.serve(handle_client, WS_HOST, WS_PORT):
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
