package com.example.ubicompp3.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.ubicompp3.model.ActuatorData
import com.example.ubicompp3.model.DetectionData
import com.example.ubicompp3.model.SensorData
import com.example.ubicompp3.network.WebSocketManager
import com.example.ubicompp3.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class EventItem(
    val type: String,
    val description: String
)

class DashboardViewModel(
    context: Context
) : ViewModel() {

    private val notificationHelper = NotificationHelper(context)

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData

    private val _detectionData = MutableStateFlow(DetectionData())
    val detectionData: StateFlow<DetectionData> = _detectionData

    private val _actuatorData = MutableStateFlow(ActuatorData())
    val actuatorData: StateFlow<ActuatorData> = _actuatorData

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _events = MutableStateFlow<List<EventItem>>(emptyList())
    val events: StateFlow<List<EventItem>> = _events

    private val _webSocketUrl = MutableStateFlow("ws://192.168.1.219:8765")
    val webSocketUrl: StateFlow<String> = _webSocketUrl

    private val _lightThreshold = MutableStateFlow("200")
    val lightThreshold: StateFlow<String> = _lightThreshold

    private val _lastCommand = MutableStateFlow("None")
    val lastCommand: StateFlow<String> = _lastCommand

    private var wsManager = createWebSocketManager()

    private fun createWebSocketManager(): WebSocketManager {
        return WebSocketManager(
            url = _webSocketUrl.value,
            onMessageReceived = { handleIncomingMessage(it) },
            onStatusChanged = { _isConnected.value = it }
        )
    }

    fun connect() {
        wsManager.connect()
    }

    fun disconnect() {
        wsManager.disconnect()
    }

    fun reconnect() {
        wsManager.disconnect()
        wsManager = createWebSocketManager()
        wsManager.connect()
    }

    fun sendSimulateAction() {
        _lastCommand.value = "simulate_actuator"
        wsManager.send("""{"type":"command","action":"simulate_actuator"}""")
    }

    fun updateWebSocketUrl(newUrl: String) {
        _webSocketUrl.value = newUrl
    }

    fun updateLightThreshold(newValue: String) {
        _lightThreshold.value = newValue
    }

    fun sendLightThreshold() {
        wsManager.send(
            """{"type":"config_update","light_threshold":${_lightThreshold.value.toIntOrNull() ?: 200}}"""
        )
    }

    private fun addEvent(event: EventItem) {
        _events.value = listOf(event) + _events.value.take(19)
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        _lastCommand.value = if (enabled) "monitoring_on" else "monitoring_off"
        wsManager.send(
            """{"type":"command","action":"set_monitoring","enabled":$enabled}"""
        )
    }

    private fun handleIncomingMessage(message: String) {
        try {
            val json = Json.parseToJsonElement(message).jsonObject
            val type = json["type"]?.jsonPrimitive?.content ?: return
            val data = json["data"]?.jsonObject ?: return

            when (type) {
                "sensor_update" -> {
                    _sensorData.value = SensorData(
                        active = data["active"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        ambient = data["ambient"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        r = data["r"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        g = data["g"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        b = data["b"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        dominant_color = data["dominant_color"]?.jsonPrimitive?.content ?: "",
                        light_state = data["light_state"]?.jsonPrimitive?.content ?: ""
                    )
                }

                "detection_event" -> {
                    val detectedColor = data["dominant_color_detected"]?.jsonPrimitive?.content ?: ""
                    val confidence = data["confidence"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val lightEvent = data["light_event"]?.jsonPrimitive?.content ?: "none"

                    _detectionData.value = DetectionData(
                        dominant_color_detected = detectedColor,
                        confidence = confidence,
                        light_event = lightEvent
                    )

                    addEvent(
                        EventItem(
                            type = "Detection",
                            description = "Color: $detectedColor | Event: $lightEvent"
                        )
                    )

                    if (lightEvent != "none") {
                        notificationHelper.showNotification(
                            title = "Light Event Detected",
                            message = "Detected color: $detectedColor, event: $lightEvent",
                            notificationId = 1001
                        )
                    }
                }

                "actuator_event" -> {
                    val action = data["action"]?.jsonPrimitive?.content ?: ""
                    val reason = data["reason"]?.jsonPrimitive?.content ?: ""

                    _actuatorData.value = ActuatorData(
                        action = action,
                        reason = reason
                    )

                    addEvent(
                        EventItem(
                            type = "Actuator",
                            description = "Action: $action | Reason: $reason"
                        )
                    )

                    notificationHelper.showNotification(
                        title = "Actuator Action",
                        message = "Action: $action, reason: $reason",
                        notificationId = 1002
                    )
                }

                "ack" -> {
                    val actionText = data["action"]?.jsonPrimitive?.content ?: ""
                    if (actionText.isNotBlank()) {
                        _lastCommand.value = actionText
                    }

                    addEvent(
                        EventItem(
                            type = "Ack",
                            description = "Backend acknowledged command"
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
    }
}