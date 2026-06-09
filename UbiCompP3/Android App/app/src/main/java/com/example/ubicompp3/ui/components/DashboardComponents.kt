package com.example.ubicompp3.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                content()
            }
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    backgroundColor: Color,
    contentColor: Color = Color.White
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MetricProgressRow(
    label: String,
    value: Int,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(value.toString(), color = Color.Gray)
        }

        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = color.copy(alpha = 0.18f)
        )
    }
}

@Composable
fun ColorPreview(color: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun dominantColorToUiColor(colorName: String): Color {
    return when (colorName.lowercase()) {
        "red" -> Color(0xFFE53935)
        "green" -> Color(0xFF43A047)
        "blue" -> Color(0xFF1E88E5)
        "neutral" -> Color(0xFF9E9E9E)
        else -> Color(0xFF9E9E9E)
    }
}

fun lightStateToColor(lightState: String): Color {
    return when (lightState.lowercase()) {
        "dark" -> Color(0xFF5C6BC0)
        "normal" -> Color(0xFFFBC02D)
        "bright" -> Color(0xFFFF9800)
        else -> Color(0xFF90A4AE)
    }
}

fun eventTypeToColor(type: String): Color {
    return when (type.lowercase()) {
        "detection" -> Color(0xFF1E88E5)
        "actuator" -> Color(0xFFE53935)
        "ack" -> Color(0xFF43A047)
        else -> Color(0xFF90A4AE)
    }
}