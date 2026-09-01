package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.must.timetable.core.util.TimeUtil
import com.must.timetable.features.timetable.domain.CustomEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSheet(
    initial: CustomEvent,
    onDismiss: () -> Unit,
    onConfirm: (CustomEvent) -> Unit,
    onDelete: (Long) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var day by remember { mutableStateOf(initial.dayOfWeek) }
    var start by remember { mutableStateOf(initial.startTime) }
    var end by remember { mutableStateOf(initial.endTime ?: "") }
    var location by remember { mutableStateOf(initial.location ?: "") }
    var notes by remember { mutableStateOf(initial.notes) }
    var repeatWeekly by remember { mutableStateOf(initial.repeatWeekly) }
    var alarm by remember { mutableStateOf(initial.alarmMinutes) }
    val isNew = initial.id == 0L

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isNew) "Add event" else "Edit event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())

            var dayExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = it }) {
                OutlinedTextField(
                    value = day, onValueChange = {}, readOnly = true, label = { Text("Day") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                    TimeUtil.DAYS.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { day = it; dayExpanded = false })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End") }, modifier = Modifier.weight(1f))
            }

            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Repeats weekly", style = MaterialTheme.typography.bodyMedium)
                    Text(if (repeatWeekly) "Every week" else "One-time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = repeatWeekly, onCheckedChange = { repeatWeekly = it })
            }

            Text("Reminder", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60).forEach { m ->
                    FilterChip(selected = alarm == m, onClick = { alarm = if (alarm == m) null else m }, label = { Text("${m}m") })
                }
                FilterChip(selected = alarm == null, onClick = { alarm = null }, label = { Text("Off") })
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                if (!isNew) {
                    TextButton(
                        onClick = { onDelete(initial.id); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Delete") }
                } else Spacer(Modifier)
                Button(onClick = {
                    if (title.isBlank()) return@Button
                    onConfirm(initial.copy(
                        title = title.trim(), dayOfWeek = day, startTime = start,
                        endTime = end.ifBlank { null }, location = location.ifBlank { null },
                        notes = notes, repeatWeekly = repeatWeekly, alarmMinutes = alarm
                    ))
                }) { Text("Save") }
            }
        }
    }
}