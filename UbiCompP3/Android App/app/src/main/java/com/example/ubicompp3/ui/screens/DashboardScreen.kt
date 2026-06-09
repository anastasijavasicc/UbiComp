package com.example.ubicompp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ubicompp3.ui.components.*
import com.example.ubicompp3.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val sensor by viewModel.sensorData.collectAsState()
    val detection by viewModel.detectionData.collectAsState()
    val actuator by viewModel.actuatorData.collectAsState()
    val connected by viewModel.isConnected.collectAsState()
    val lastCommand by viewModel.lastCommand.collectAsState()

    val dashboardColor = dominantColorToUiColor(
        if (detection.dominant_color_detected.isNotBlank()) {
            detection.dominant_color_detected
        } else {
            sensor.dominant_color
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ){
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "IoT Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Real-time sensor monitoring",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            StatusChip(
                text = if (connected) "Connected" else "Disconnected",
                backgroundColor = if (connected) Color(0xFF43A047) else Color(0xFFE53935)
            )
        }

        SectionCard(title = "System Status") {
            InfoRow("Connection", if (connected) "Connected" else "Disconnected")
            InfoRow("Monitoring", if (sensor.active == 1) "ON" else "OFF")
            InfoRow("Last command", lastCommand)
        }

        SectionCard(title = "Ambient Light") {
            val ambientProgress = (sensor.ambient / 1200f).coerceIn(0f, 1f)
            val lightColor = lightStateToColor(sensor.light_state)

            InfoRow("Ambient value", sensor.ambient.toString())
            InfoRow(
                "Light state",
                sensor.light_state.replaceFirstChar { it.uppercase() }
            )

            LinearProgressIndicator(
                progress = ambientProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = lightColor,
                trackColor = lightColor.copy(alpha = 0.18f)
            )
        }

        SectionCard(title = "Detected Color") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorPreview(
                    color = dominantColorToUiColor(sensor.dominant_color),
                    label = sensor.dominant_color.replaceFirstChar { it.uppercase() }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricProgressRow(
                        label = "Red",
                        value = sensor.r,
                        progress = sensor.r / 255f,
                        color = Color(0xFFE53935)
                    )
                    MetricProgressRow(
                        label = "Green",
                        value = sensor.g,
                        progress = sensor.g / 255f,
                        color = Color(0xFF43A047)
                    )
                    MetricProgressRow(
                        label = "Blue",
                        value = sensor.b,
                        progress = sensor.b / 255f,
                        color = Color(0xFF1E88E5)
                    )
                }
            }
        }

        SectionCard(title = "AI Detection") {
            InfoRow(
                "Detected color",
                detection.dominant_color_detected.ifBlank { "No data" }
            )
            InfoRow(
                "Confidence",
                "${(detection.confidence * 100).toInt()}%"
            )

            LinearProgressIndicator(
                progress = detection.confidence.toFloat().coerceIn(0f, 1f) ,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = dashboardColor,
                trackColor = dashboardColor.copy(alpha = 0.18f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            StatusChip(
                text = if (detection.light_event == "none") "No light event" else detection.light_event,
                backgroundColor = if (detection.light_event == "none") {
                    Color(0xFF90A4AE)
                } else {
                    Color(0xFFFF9800)
                }
            )
        }

        SectionCard(title = "Actuator Action") {
            InfoRow(
                "Action",
                actuator.action.ifBlank { "No action" }
            )
            InfoRow(
                "Reason",
                actuator.reason.ifBlank { "No reason" }
            )

            StatusChip(
                text = if (actuator.action.isBlank()) "Idle" else "Triggered",
                backgroundColor = if (actuator.action.isBlank()) {
                    Color(0xFF90A4AE)
                } else {
                    Color(0xFFE53935)
                }
            )
        }

        Button(
            onClick = { viewModel.sendSimulateAction() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )
        ) {
            Text("Simulate actuator action")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}