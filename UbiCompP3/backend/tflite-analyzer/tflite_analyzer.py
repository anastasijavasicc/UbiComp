import json
import numpy as np
import paho.mqtt.client as mqtt
from tflite_runtime.interpreter import Interpreter

MQTT_BROKER = "mosquitto"
MQTT_PORT = 1883

SENSOR_TOPIC = "iot/sensors/light_color"
EVENT_TOPIC = "iot/events/detection"
ACTUATOR_TOPIC = "iot/actuator/action"
CONFIG_TOPIC = "iot/app/config"

MODEL_PATH = "/app/color_classifier.tflite"
LABEL_MAP_PATH = "/app/label_map.json"
SCALER_MEAN_PATH = "/app/scaler_mean.npy"
SCALER_SCALE_PATH = "/app/scaler_scale.npy"

previous_ambient = None
light_threshold = 200

interpreter = Interpreter(model_path=MODEL_PATH)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

with open(LABEL_MAP_PATH, "r") as f:
    label_map = json.load(f)

scaler_mean = np.load(SCALER_MEAN_PATH)
scaler_scale = np.load(SCALER_SCALE_PATH)


def preprocess_features(ambient, r, g, b):
    x = np.array([[ambient, r, g, b]], dtype=np.float32)
    x_scaled = (x - scaler_mean) / scaler_scale
    return x_scaled.astype(np.float32)


def run_tflite_inference(ambient, r, g, b):
    x = preprocess_features(ambient, r, g, b)

    interpreter.set_tensor(input_details[0]["index"], x)
    interpreter.invoke()

    output = interpreter.get_tensor(output_details[0]["index"])[0]
    pred_index = int(np.argmax(output))
    pred_label = label_map[str(pred_index)]
    confidence = float(output[pred_index])

    return pred_label, confidence


def detect_light_change(current_ambient):
    global previous_ambient, light_threshold

    if previous_ambient is None:
        previous_ambient = current_ambient
        return None

    diff = current_ambient - previous_ambient
    previous_ambient = current_ambient

    if diff > light_threshold:
        return "sudden_brightness_increase"
    elif diff < -light_threshold:
        return "sudden_brightness_decrease"
    else:
        return None


def on_connect(client, userdata, flags, rc, properties=None):
    print(f"Connected to MQTT broker with code {rc}")
    client.subscribe(SENSOR_TOPIC)
    client.subscribe(CONFIG_TOPIC)


def on_message(client, userdata, msg):
    global light_threshold

    try:
        payload = msg.payload.decode("utf-8")
        data = json.loads(payload)

        if msg.topic == CONFIG_TOPIC:
            new_threshold = data.get("light_threshold")
            if new_threshold is not None:
                light_threshold = int(new_threshold)
                print(f"Updated light threshold to: {light_threshold}")
            return

        if "ambient" not in data or "r" not in data or "g" not in data or "b" not in data:
            print("Skipping non-sensor event message.")
            return

        ambient = int(data.get("ambient", 0))
        r = int(data.get("r", 0))
        g = int(data.get("g", 0))
        b = int(data.get("b", 0))

        dominant_color_detected, confidence = run_tflite_inference(ambient, r, g, b)
        light_event = detect_light_change(ambient)

        result = {
            "ambient": ambient,
            "r": r,
            "g": g,
            "b": b,
            "dominant_color_detected": dominant_color_detected,
            "confidence": round(confidence, 4),
            "light_event": light_event if light_event else "none",
            "light_threshold": light_threshold
        }

        print("TFLITE ANALYZER RESULT:")
        print(json.dumps(result, indent=2))

        client.publish(EVENT_TOPIC, json.dumps(result))

        if light_event is not None:
            actuator_msg = {
                "action": "simulated_alert",
                "reason": light_event,
                "ambient": ambient,
                "dominant_color_detected": dominant_color_detected,
                "light_threshold": light_threshold
            }
            client.publish(ACTUATOR_TOPIC, json.dumps(actuator_msg))
            print("SIMULATED ACTUATOR ACTION:")
            print(json.dumps(actuator_msg, indent=2))

        print("-" * 60)

    except Exception as e:
        print(f"Error processing MQTT message: {e}")


def main():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.loop_forever()


if __name__ == "__main__":
    main()
