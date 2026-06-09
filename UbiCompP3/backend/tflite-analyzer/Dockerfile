FROM python:3.11-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY tflite_runtime-2.14.0-cp311-cp311-manylinux_2_34_aarch64.whl .
RUN pip install --no-cache-dir ./tflite_runtime-2.14.0-cp311-cp311-manylinux_2_34_aarch64.whl

COPY tflite_analyzer.py .
COPY color_classifier.tflite .
COPY label_map.json .
COPY scaler_mean.npy .
COPY scaler_scale.npy .

CMD ["python", "tflite_analyzer.py"]
