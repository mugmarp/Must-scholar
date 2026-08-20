package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.must.timetable.features.timetable.domain.TimetableEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureDetailSheet(
    entry: TimetableEntry,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onAlarmToggle: (Int) -> Unit,
    selectedAlarmMinutes: Int?
) {
    ModalBottomSheet(onDismissRequest = {}) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(entry.courseTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${entry.courseCode} - ${entry.dayOfWeek} ${entry.timeSlot}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            DetailRow("Room", entry.room)
            DetailRow("Lecturer", entry.lecturer.ifEmpty { "Not assigned" })
            DetailRow("Session Type", entry.sessionType ?: "Theory")
            if (entry.sharedGroupsList.isNotEmpty()) {
                DetailRow("Shared with", entry.sharedGroupsList.joinToString(", "))
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Reminders", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60).forEach { minutes ->
                    FilterChip(
                        selected = selectedAlarmMinutes == minutes,
                        onClick = { onAlarmToggle(minutes) },
                        label = { Text("${minutes}m before") },
                        leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) }
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Personal Notes", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = noteText, onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add notes for this lecture...") }, minLines = 3)
            Text("Linked via: ${entry.naturalKey}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}