package com.example.ubicompp3.model

data class SensorData(
    val active: Int = 0,
    val ambient: Int = 0,
    val r: Int = 0,
    val g: Int = 0,
    val b: Int = 0,
    val dominant_color: String = "",
    val light_state: String = ""
)

data class DetectionData(
    val dominant_color_detected: String = "",
    val confidence: Double = 0.0,
    val light_event: String = "none"
)

data class ActuatorData(
    val action: String = "",
    val reason: String = ""
)

data class IncomingMessage<T>(
    val type: String,
    val data: T
)