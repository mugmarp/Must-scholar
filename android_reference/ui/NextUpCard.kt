package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.must.timetable.features.timetable.domain.TimetableEntry

@Composable
fun NextUpCard(entries: List<TimetableEntry>) {
    val nextUp = entries.firstOrNull()

    if (nextUp == null) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text("No more classes today", modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val sessionColor = sessionTypeColor(nextUp.sessionType)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("NEXT UP", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Surface(shape = RoundedCornerShape(8.dp), color = sessionColor) {
                    Text(nextUp.sessionType ?: "THEORY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(nextUp.courseTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${nextUp.courseCode} - ${nextUp.timeSlot}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Room: ${nextUp.room}", style = MaterialTheme.typography.bodyMedium)
                if (nextUp.lecturer.isNotEmpty()) {
                    Text("Lecturer: ${nextUp.lecturer}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Starts in 2h 15m", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun sessionTypeColor(type: String?): Color = when (type?.uppercase()) {
    "THEORY" -> Color(0xFF3B82F6)
    "PRACTICAL" -> Color(0xFF22C55E)
    "CLINICAL", "WARD" -> Color(0xFFF59E0B)
    else -> Color(0xFF6B7280)
}