package com.must.timetable.features.tasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.must.timetable.features.timetable.domain.Assignment
import com.must.timetable.features.timetable.domain.Priority
import java.text.SimpleDateFormat
import java.util.Locale

private val REMINDER_OPTIONS = listOf(0, 15, 30, 60, 120, 1440)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentSheet(
    initial: Assignment,
    onDismiss: () -> Unit,
    onConfirm: (Assignment) -> Unit,
    onDelete: (Long) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var course by remember { mutableStateOf(initial.courseCode ?: "") }
    var dueLocal by remember { mutableStateOf(initial.dueAtMillis?.let { millisToLocal(it) } ?: "") }
    var reminder by remember { mutableStateOf(initial.reminderMinutes ?: 0) }
    var priority by remember { mutableStateOf(initial.priority) }
    var notes by remember { mutableStateOf(initial.notes) }
    var completed by remember { mutableStateOf(initial.completed) }
    val isNew = initial.id == 0L

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isNew) "New task" else "Edit task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Course code") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = dueLocal, onValueChange = { dueLocal = it },
                label = { Text("Due (yyyy-MM-dd HH:mm)") }, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var pExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = pExp, onExpandedChange = { pExp = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = priority.name, onValueChange = {}, readOnly = true, label = { Text("Priority") }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = pExp, onDismissRequest = { pExp = false }) {
                        Priority.values().forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { priority = it; pExp = false }) }
                    }
                }
                var rExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = rExp, onExpandedChange = { rExp = it }, modifier = Modifier.weight(1f)) {
                    val rLabel = if (reminder == 0) "Off" else "${reminder}m before"
                    OutlinedTextField(value = rLabel, onValueChange = {}, readOnly = true, label = { Text("Reminder") }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = rExp, onDismissRequest = { rExp = false }) {
                        REMINDER_OPTIONS.forEach { m ->
                            DropdownMenuItem(text = { Text(if (m == 0) "Off" else "$m min before") }, onClick = { reminder = m; rExp = false })
                        }
                    }
                }
            }
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = completed, onCheckedChange = { completed = it })
                Text("Completed")
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                if (!isNew) {
                    TextButton(onClick = { onDelete(initial.id); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Delete")
                    }
                } else Spacer(Modifier)
                Button(onClick = {
                    if (title.isBlank()) return@Button
                    val due = localToMillis(dueLocal)
                    onConfirm(initial.copy(
                        title = title.trim(),
                        courseCode = course.ifBlank { null },
                        dueAtMillis = due,
                        reminderMinutes = if (reminder == 0) null else reminder,
                        priority = priority,
                        notes = notes,
                        completed = completed
                    ))
                }) { Text("Save") }
            }
        }
    }
}

private fun millisToLocal(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(millis))

private fun localToMillis(local: String): Long? = runCatching {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(local)?.time
}.getOrNull()