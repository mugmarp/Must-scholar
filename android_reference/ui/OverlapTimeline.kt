package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.must.timetable.features.timetable.domain.TimetableEntry

@Composable
fun OverlapTimeline(
    entries: List<TimetableEntry>,
    onLectureClick: (TimetableEntry) -> Unit
) {
    val groupedByTime = entries.groupBy { it.startTime }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groupedByTime.entries.toList()) { (timeSlot, lecturesAtTime) ->
            TimeSlotRow(timeLabel = timeSlot, lectures = lecturesAtTime, onLectureClick = onLectureClick)
        }
    }
}

@Composable
fun TimeSlotRow(
    timeLabel: String,
    lectures: List<TimetableEntry>,
    onLectureClick: (TimetableEntry) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(timeLabel, modifier = Modifier.width(56.dp).padding(top = 12.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        if (lectures.size > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                lectures.forEach { lecture ->
                    LectureCard(entry = lecture, onClick = { onLectureClick(lecture) },
                        modifier = Modifier.weight(1f))
                }
            }
        } else {
            LectureCard(entry = lectures.first(), onClick = { onLectureClick(lectures.first()) },
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun LectureCard(
    entry: TimetableEntry, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val sessionColor = sessionTypeColor(entry.sessionType)
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(sessionColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.height(8.dp))
            Text(entry.courseCode, style = MaterialTheme.typography.labelSmall,
                color = sessionColor, fontWeight = FontWeight.Bold)
            Text(entry.courseTitle, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Room: ${entry.room}", style = MaterialTheme.typography.labelSmall)
                if (entry.lecturer.isNotEmpty()) {
                    Text(entry.lecturer.take(15), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (entry.sharedGroupsList.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Shared: ${entry.sharedGroupsList.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}