import json
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler
import tensorflow as tf

DATA_PATH = "color_training_data.csv"
MODEL_OUT = "color_classifier.tflite"
LABELS_OUT = "label_map.json"
SCALER_MEAN_OUT = "scaler_mean.npy"
SCALER_SCALE_OUT = "scaler_scale.npy"

# Ucitavanje podataka
df = pd.read_csv(DATA_PATH)

X = df[["ambient", "r", "g", "b"]].values.astype(np.float32)
y = df["label"].values

# Label encoding
label_encoder = LabelEncoder()
y_encoded = label_encoder.fit_transform(y)

# Standardizacija
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X).astype(np.float32)

# Train/test split
X_train, X_test, y_train, y_test = train_test_split(
    X_scaled, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
)

# Model
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(4,)),
    tf.keras.layers.Dense(16, activation="relu"),
    tf.keras.layers.Dense(16, activation="relu"),
    tf.keras.layers.Dense(len(label_encoder.classes_), activation="softmax")
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

# Trening
model.fit(
    X_train, y_train,
    validation_data=(X_test, y_test),
    epochs=50,
    batch_size=8,
    verbose=1
)

# Evaluacija
loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"Test accuracy: {accuracy:.4f}")

# TFLite export
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open(MODEL_OUT, "wb") as f:
    f.write(tflite_model)

# Sacuvaj label map
label_map = {int(i): label for i, label in enumerate(label_encoder.classes_)}
with open(LABELS_OUT, "w") as f:
    json.dump(label_map, f, indent=2)

# Sacuvaj scaler parametre
np.save(SCALER_MEAN_OUT, scaler.mean_)
np.save(SCALER_SCALE_OUT, scaler.scale_)

print("Training complete.")
print(f"TFLite model saved to: {MODEL_OUT}")
print(f"Label map saved to: {LABELS_OUT}")
print(f"Scaler mean saved to: {SCALER_MEAN_OUT}")
print(f"Scaler scale saved to: {SCALER_SCALE_OUT}")