package com.example.ubicompp3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ubicompp3.ui.components.eventTypeToColor
import com.example.ubicompp3.viewmodel.DashboardViewModel

@Composable
fun EventsScreen(viewModel: DashboardViewModel) {
    val events by viewModel.events.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(16.dp)
    ) {
        Text(
            "Recent Events",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp))
        {
            items(events) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(56.dp)
                                .background(eventTypeToColor(event.type))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                event.type,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                event.description,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}