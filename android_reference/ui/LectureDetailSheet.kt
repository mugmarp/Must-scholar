package com.must.timetable.features.timetable.ui

/**
 * Mirrors src/components/timetable/LectureDetailSheet.jsx:
 * session badge, icon detail rows, 15/30/60 reminder buttons,
 * note field with natural-key caption and Save note button.
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.timetable.features.timetable.domain.TimetableEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureDetailSheet(
    entry: TimetableEntry,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onAlarmToggle: (Int) -> Unit,
    selectedAlarmMinutes: Int?,
    onSaveNote: () -> Unit = {}
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val style = sessionStyle(entry)

    ModalBottomSheet(onDismissRequest = {}) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // title + subtitle
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.courseTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${entry.courseCode} • ${entry.dayOfWeek} ${entry.timeSlot}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
            }

            // session badge
            Text(
                style.label,
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(style.badge)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )

            // detail rows with icons (mirror the web DetailRow structure)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(Icons.Default.LocationOn, "Room", entry.room.ifEmpty { "—" })
                DetailRow(Icons.Default.Person, "Lecturer", entry.lecturer.ifEmpty { "Not assigned" })
                DetailRow(Icons.Default.Schedule, "Time", "${entry.startTime} – ${entry.endTime}")
            }

            if (entry.sharedGroupsList.isNotEmpty()) {
                Text(
                    "Shared with: ${entry.sharedGroupsList.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = onSurfaceVariant
                )
            }

            // reminders: filled/outlined toggle buttons (web uses Button variants)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { minutes ->
                        val selected = selectedAlarmMinutes == minutes
                        val shape = RoundedCornerShape(50)
                        val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        if (selected) {
                            Button(onClick = { onAlarmToggle(minutes) }, shape = shape, contentPadding = contentPadding) {
                                Text("${minutes}m before", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(onClick = { onAlarmToggle(minutes) }, shape = shape, contentPadding = contentPadding) {
                                Text("${minutes}m before", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Text(
                    "Alarm fires before class starts.",
                    fontSize = 11.sp,
                    color = onSurfaceVariant
                )
            }

            // notes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Personal notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add notes for this lecture...") },
                    minLines = 4
                )
                Text(
                    "Linked by: ${entry.naturalKey}",
                    fontSize = 10.sp,
                    color = onSurfaceVariant
                )
                Button(onClick = onSaveNote) { Text("Save note") }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}