#define EIDSP_QUANTIZE_FILTERBANK 0
#define EI_CLASSIFIER_SLICES_PER_MODEL_WINDOW 4

#include <PDM.h>
#include <Arduino_APDS9960.h>
#include <anastasijavasicc-project-1_inferencing.h>

#define LED_PIN_RED 22
#define LED_PIN_GREEN 24
#define LED_PIN_BLUE 23
#define LED_PIN 13

typedef struct {
    signed short *buffers[2];
    unsigned char buf_select;
    unsigned char buf_ready;
    unsigned int buf_count;
    unsigned int n_samples;
} inference_t;

static inference_t inference;
static bool record_ready = false;
static signed short *sampleBuffer;
static bool debug_nn = false;
static int print_results = -(EI_CLASSIFIER_SLICES_PER_MODEL_WINDOW);

static bool active = false;

const float COMMAND_THRESHOLD = 0.60f;
unsigned long lastSensorSendTime = 0;
const unsigned long sendIntervalMs = 1000;

// za detekciju nagle promene osvetljenja
int previousAmbient = -1;
unsigned long lastRedEventTime = 0;
const unsigned long redHoldMs = 1000;

// RGB LED je active-low
void setColor(bool red, bool green, bool blue) {
  digitalWrite(LED_PIN_RED, red);
  digitalWrite(LED_PIN_GREEN, green);
  digitalWrite(LED_PIN_BLUE, blue);
}

void setRgbOff() {
  setColor(HIGH, HIGH, HIGH);
}

void setRgbGreen() {
  setColor(HIGH, HIGH, LOW);
}

void setRgbBlue() {
  setColor(HIGH, LOW, HIGH);
}

void setRgbRed() {
  setColor(LOW, HIGH, HIGH);
}

String getDominantColor(int r, int g, int b) {
  int maxVal = max(r, max(g, b));

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

bool detectSuddenLightChange(int ambient) {
  if (previousAmbient == -1) {
    previousAmbient = ambient;
    return false;
  }

  int diff = ambient - previousAmbient;
  previousAmbient = ambient;

  if (diff > 200 || diff < -200) {
    return true;
  }

  return false;
}

void setup() {
  Serial.begin(115200);
  while (!Serial);

  pinMode(LED_PIN_RED, OUTPUT);
  pinMode(LED_PIN_GREEN, OUTPUT);
  pinMode(LED_PIN_BLUE, OUTPUT);
  pinMode(LED_PIN, OUTPUT);

  digitalWrite(LED_PIN, LOW);
  setRgbBlue();

  if (!APDS.begin()) {
    Serial.println("{\"error\":\"Failed to initialize APDS9960\"}");
    while (1);
  }

  run_classifier_init();

  if (microphone_inference_start(EI_CLASSIFIER_SLICE_SIZE) == false) {
    Serial.println("{\"error\":\"Failed to start microphone inference\"}");
    return;
  }

  Serial.println("{\"status\":\"System initialized\",\"voice_trigger\":\"enabled\",\"active\":0}");
}

void loop() {
  bool m = microphone_inference_record();
  if (!m) {
    Serial.println("{\"error\":\"Failed to record audio\"}");
    return;
  }

  signal_t signal;
  signal.total_length = EI_CLASSIFIER_SLICE_SIZE;
  signal.get_data = &microphone_audio_signal_get_data;

  ei_impulse_result_t result = {0};
  EI_IMPULSE_ERROR r = run_classifier_continuous(&signal, &result, debug_nn);

  if (r != EI_IMPULSE_OK) {
    Serial.println("{\"error\":\"Classifier failed\"}");
    return;
  }

  if (++print_results >= EI_CLASSIFIER_SLICES_PER_MODEL_WINDOW) {
    float goScore = 0.0f;
    float stopScore = 0.0f;
    float unknownScore = 0.0f;

    for (size_t ix = 0; ix < EI_CLASSIFIER_LABEL_COUNT; ix++) {
      const char* label = result.classification[ix].label;
      float value = result.classification[ix].value;

      if (strcmp(label, "go") == 0) {
        goScore = value;
      }
      else if (strcmp(label, "stop") == 0) {
        stopScore = value;
      }
      else if (strcmp(label, "unknown") == 0) {
        unknownScore = value;
      }
    }

    if (goScore > COMMAND_THRESHOLD &&
        goScore > stopScore &&
        goScore > unknownScore) {
      if (!active) {
        active = true;
        digitalWrite(LED_PIN, HIGH);
        setRgbGreen();
        Serial.println("{\"event\":\"voice_trigger\",\"command\":\"go\",\"active\":1}");
      }
    }

    if (stopScore > COMMAND_THRESHOLD &&
        stopScore > goScore &&
        stopScore > unknownScore) {
      if (active) {
        active = false;
        digitalWrite(LED_PIN, LOW);
        setRgbBlue();
        Serial.println("{\"event\":\"voice_trigger\",\"command\":\"stop\",\"active\":0}");
      }
    }

    print_results = 0;
  }

  if (active && millis() - lastSensorSendTime > sendIntervalMs) {
    if (APDS.colorAvailable()) {
      int r, g, b, ambient;
      APDS.readColor(r, g, b, ambient);

      String dominantColor = getDominantColor(r, g, b);
      String lightState = getLightState(ambient);

      bool suddenLightChange = detectSuddenLightChange(ambient);

      if (suddenLightChange) {
        setRgbRed();
        lastRedEventTime = millis();
      } else {
        if (millis() - lastRedEventTime > redHoldMs) {
          setRgbGreen();
        }
      }

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

      lastSensorSendTime = millis();
    }
  }
}

static void pdm_data_ready_inference_callback(void)
{
  int bytesAvailable = PDM.available();
  int bytesRead = PDM.read((char *)&sampleBuffer[0], bytesAvailable);

  if (record_ready == true) {
    for (int i = 0; i < (bytesRead >> 1); i++) {
      inference.buffers[inference.buf_select][inference.buf_count++] = sampleBuffer[i];

      if (inference.buf_count >= inference.n_samples) {
        inference.buf_select ^= 1;
        inference.buf_count = 0;
        inference.buf_ready = 1;
      }
    }
  }
}

static bool microphone_inference_start(uint32_t n_samples)
{
  inference.buffers[0] = (signed short *)malloc(n_samples * sizeof(signed short));
  if (inference.buffers[0] == NULL) {
    return false;
  }

  inference.buffers[1] = (signed short *)malloc(n_samples * sizeof(signed short));
  if (inference.buffers[1] == NULL) {
    free(inference.buffers[0]);
    return false;
  }

  sampleBuffer = (signed short *)malloc((n_samples >> 1) * sizeof(signed short));
  if (sampleBuffer == NULL) {
    free(inference.buffers[0]);
    free(inference.buffers[1]);
    return false;
  }

  inference.buf_select = 0;
  inference.buf_count = 0;
  inference.n_samples = n_samples;
  inference.buf_ready = 0;

  PDM.onReceive(&pdm_data_ready_inference_callback);
  PDM.setBufferSize((n_samples >> 1) * sizeof(int16_t));

  if (!PDM.begin(1, EI_CLASSIFIER_FREQUENCY)) {
    return false;
  }

  PDM.setGain(127);
  record_ready = true;

  return true;
}

static bool microphone_inference_record(void)
{
  bool ret = true;

  if (inference.buf_ready == 1) {
    ret = false;
  }

  while (inference.buf_ready == 0) {
    delay(1);
  }

  inference.buf_ready = 0;
  return ret;
}

static int microphone_audio_signal_get_data(size_t offset, size_t length, float *out_ptr)
{
  numpy::int16_to_float(&inference.buffers[inference.buf_select ^ 1][offset], out_ptr, length);
  return 0;
}

static void microphone_inference_end(void)
{
  PDM.end();
  free(inference.buffers[0]);
  free(inference.buffers[1]);
  free(sampleBuffer);
}

#if !defined(EI_CLASSIFIER_SENSOR) || EI_CLASSIFIER_SENSOR != EI_CLASSIFIER_SENSOR_MICROPHONE
#error "Invalid model for current sensor."
#endif
