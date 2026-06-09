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
import com.example.ubicompp3.viewmodel.DashboardViewModel

@Composable
fun SettingsScreen(viewModel: DashboardViewModel) {
    val wsUrl by viewModel.webSocketUrl.collectAsState()
    val threshold by viewModel.lightThreshold.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        SectionCard(title = "Connection") {
            OutlinedTextField(
                value = wsUrl,
                onValueChange = { viewModel.updateWebSocketUrl(it) },
                label = { Text("WebSocket URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            Button(
                onClick = { viewModel.reconnect() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("Apply and reconnect")
            }
        }

        SectionCard(title = "Detection Threshold") {
            OutlinedTextField(
                value = threshold,
                onValueChange = { viewModel.updateLightThreshold(it) },
                label = { Text("Light threshold") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            Button(
                onClick = { viewModel.sendLightThreshold() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("Send threshold to backend")
            }
        }

        SectionCard(title = "About") {
            Text("Project: UbiCompP3")
            Text("Type: IoT mobile dashboard")
            Text("Protocol: WebSocket")
        }
    }
}