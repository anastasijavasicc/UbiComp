#define EIDSP_QUANTIZE_FILTERBANK 0
#define EI_CLASSIFIER_SLICES_PER_MODEL_WINDOW 4

/*
  Project 1 - TinyML voice-controlled monitoring system
  Hardware: Arduino Nano 33 BLE Sense Lite
  Voice commands:
    - "go"   -> starts monitoring
    - "stop" -> stops monitoring

  Active components:
    - built-in microphone
    - IMU accelerometer
    - barometer
    - RGB LED + built-in LED

  Behavior:
    - idle state: blue RGB LED
    - monitoring active: green RGB LED
    - motion detected: red RGB LED
*/

#include <PDM.h>
#include <Arduino_LSM9DS1.h> // biblioteka za akcelerometar
#include <Arduino_LPS22HB.h> // biblioteka za barometar
#include <anastasijavasicc-project-1_inferencing.h>
#include <math.h>

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

// System state: true when monitoring is active
static bool monitoring_active = false;

// Command recognition and motion detection thresholds
const float COMMAND_THRESHOLD = 0.70f;
const float MOTION_THRESHOLD = 1.20f;

unsigned long lastPressurePrintTime = 0;
const unsigned long pressurePrintIntervalMs = 1500;

// active-low RGB
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

// Updates RGB LED color depending on system state and motion detection
void applySystemColor(bool motionDetected) {
    if (monitoring_active) {
        if (motionDetected) {
            setRgbRed();
        } else {
            setRgbGreen();
        }
    } else {
        setRgbBlue();
    }
}

void setup()
{
    Serial.begin(115200);
    while (!Serial);

    Serial.println("TinyML voice-controlled monitoring system");

    pinMode(LED_PIN_RED, OUTPUT);
    pinMode(LED_PIN_GREEN, OUTPUT);
    pinMode(LED_PIN_BLUE, OUTPUT);
    pinMode(LED_PIN, OUTPUT);

    digitalWrite(LED_PIN, LOW);
    setRgbOff();

    if (!IMU.begin()) {
        Serial.println("Failed to initialize IMU!");
        while (1);
    }

    if (!BARO.begin()) {
        Serial.println("Failed to initialize barometer!");
        while (1);
    }

    Serial.println("IMU initialized");
    Serial.println("Barometer initialized");

    Serial.println("Inferencing settings:");
    Serial.print("\tInterval: ");
    Serial.print((float)EI_CLASSIFIER_INTERVAL_MS, 2);
    Serial.println(" ms.");
    Serial.print("\tFrame size: ");
    Serial.println(EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE);
    Serial.print("\tSample length: ");
    Serial.print(EI_CLASSIFIER_RAW_SAMPLE_COUNT / 16);
    Serial.println(" ms.");
    Serial.print("\tNo. of classes: ");
    Serial.println(sizeof(ei_classifier_inferencing_categories) / sizeof(ei_classifier_inferencing_categories[0]));

    run_classifier_init();

    if (microphone_inference_start(EI_CLASSIFIER_SLICE_SIZE) == false) {
        Serial.print("ERR: Could not allocate audio buffer (size ");
        Serial.print(EI_CLASSIFIER_RAW_SAMPLE_COUNT);
        Serial.println(")");
        return;
    }

    monitoring_active = false;
    digitalWrite(LED_PIN, LOW);
    setRgbBlue();

    Serial.println("System is idle. Say 'go' to begin monitoring.");
}

void loop()
{
    bool m = microphone_inference_record();
    if (!m) {
        Serial.println("ERR: Failed to record audio...");
        return;
    }

    signal_t signal;
    signal.total_length = EI_CLASSIFIER_SLICE_SIZE;
    signal.get_data = &microphone_audio_signal_get_data;

    ei_impulse_result_t result = {0};
    EI_IMPULSE_ERROR r = run_classifier_continuous(&signal, &result, debug_nn);

    if (r != EI_IMPULSE_OK) {
        Serial.print("ERR: Failed to run classifier (");
        Serial.print(r);
        Serial.println(")");
        return;
    }

    if (++print_results >= EI_CLASSIFIER_SLICES_PER_MODEL_WINDOW) {
        float goScore = 0.0f;
        float stopScore = 0.0f;
        float unknownScore = 0.0f;

        Serial.print("Predictions (DSP: ");
        Serial.print(result.timing.dsp);
        Serial.print(" ms, Classification: ");
        Serial.print(result.timing.classification);
        Serial.println(" ms):");

        for (size_t ix = 0; ix < EI_CLASSIFIER_LABEL_COUNT; ix++) {
            const char* label = result.classification[ix].label;
            float value = result.classification[ix].value;

            Serial.print("    ");
            Serial.print(label);
            Serial.print(": ");
            Serial.println(value, 5);

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

        // GO
        if (goScore > COMMAND_THRESHOLD &&
            goScore > stopScore &&
            goScore > unknownScore) {

            if (!monitoring_active) {
                monitoring_active = true;
                digitalWrite(LED_PIN, HIGH);
                Serial.println("Monitoring started");
            }
        }

        // STOP
        if (stopScore > COMMAND_THRESHOLD &&
            stopScore > goScore &&
            stopScore > unknownScore) {

            if (monitoring_active) {
                monitoring_active = false;
                digitalWrite(LED_PIN, LOW);
                Serial.println("Monitoring stopped");
            }
        }

        bool motionDetected = false;

        if (monitoring_active) {
            float x, y, z;
            if (IMU.accelerationAvailable()) {
                IMU.readAcceleration(x, y, z);

                float magnitude = sqrt(x * x + y * y + z * z);

                Serial.print("Acceleration X: ");
                Serial.print(x);
                Serial.print(" Y: ");
                Serial.print(y);
                Serial.print(" Z: ");
                Serial.print(z);
                Serial.print(" | Magnitude: ");
                Serial.println(magnitude);

                if (magnitude > MOTION_THRESHOLD) {
                    motionDetected = true;
                    Serial.println("Motion detected");
                }
            }

            if (millis() - lastPressurePrintTime > pressurePrintIntervalMs) {
                float pressure = BARO.readPressure();
                Serial.print("Pressure: ");
                Serial.println(pressure);
                lastPressurePrintTime = millis();
            }
        }

        applySystemColor(motionDetected);

        print_results = 0;
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
        Serial.println("Failed to start PDM!");
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
        Serial.println("Error sample buffer overrun. Decrease slices per model window.");
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