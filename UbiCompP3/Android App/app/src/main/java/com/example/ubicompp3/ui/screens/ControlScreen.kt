package com.example.ubicompp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ubicompp3.ui.components.SectionCard
import com.example.ubicompp3.ui.components.StatusChip
import com.example.ubicompp3.viewmodel.DashboardViewModel

@Composable
fun ControlScreen(viewModel: DashboardViewModel) {
    val connected by viewModel.isConnected.collectAsState()
    val actuator by viewModel.actuatorData.collectAsState()
    val sensor by viewModel.sensorData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Control Center",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        StatusChip(
            text = if (connected) "Connected to backend" else "Disconnected",
            backgroundColor = if (connected) Color(0xFF43A047) else Color(0xFFE53935)
        )

        SectionCard(title = "Monitoring") {
            Text(
                if (sensor.active == 1) "Monitoring is currently active"
                else "Monitoring is currently inactive"
            )
        }

        SectionCard(title = "Actions") {
            Button(
                onClick = { viewModel.setMonitoringEnabled(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) {
                Text("Enable Monitoring")
            }

            Button(
                onClick = { viewModel.setMonitoringEnabled(false) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("Disable Monitoring")
            }

            Button(
                onClick = { viewModel.sendSimulateAction() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("Simulate actuator action")
            }

            OutlinedButton(
                onClick = { viewModel.reconnect() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reconnect to backend")
            }
        }

        SectionCard(title = "Last Actuator Event") {
            Text("Action: ${actuator.action.ifBlank { "No action" }}")
            Text("Reason: ${actuator.reason.ifBlank { "No reason" }}")
        }
    }
}