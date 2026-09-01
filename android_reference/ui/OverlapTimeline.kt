package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.TimetableEntry

@Composable
fun DayTimeline(
    items: List<DayItem>,
    onLecture: (TimetableEntry) -> Unit,
    onEvent: (CustomEvent) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No classes or events for this day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            when (item) {
                is DayItem.Lecture -> LectureCard(
                    entry = item.entry,
                    onClick = { onLecture(item.entry) },
                    modifier = Modifier.fillMaxWidth()
                )
                is DayItem.Event -> EventCard(
                    event = item.event,
                    onClick = { onEvent(item.event) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun EventCard(event: CustomEvent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
        border = BorderStroke(1.dp, Color(0xFFA855F7))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("PERSONAL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                if (event.alarmMinutes != null) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            val meta = buildString {
                append(event.startTime)
                event.endTime?.let { append("–$it") }
                event.location?.let { append(" • $it") }
            }
            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LectureCard(entry: TimetableEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sessionColor = sessionTypeColor(entry.sessionType)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(sessionColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.height(8.dp))
            Text(entry.courseCode, style = MaterialTheme.typography.labelSmall, color = sessionColor, fontWeight = FontWeight.Bold)
            Text(entry.courseTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Room: ${entry.room}", style = MaterialTheme.typography.labelSmall)
                if (entry.lecturer.isNotEmpty()) {
                    Text(entry.lecturer.take(15), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}