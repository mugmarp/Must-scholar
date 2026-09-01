package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.timetable.AppGraph
import com.must.timetable.core.alarm.ReminderScheduler
import com.must.timetable.core.util.TimeUtil
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimetableRoute() {
    val context = LocalContext.current
    val vm: TimetableViewModel = viewModel(factory = AppGraph.factory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val repo = remember { AppGraph.repository(context) }
    val scope = rememberCoroutineScope()

    var detailEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var editingEvent by remember { mutableStateOf<CustomEvent?>(null) }
    var pendingConflict by remember { mutableStateOf<CustomEvent?>(null) }

    val initialIndex = TimeUtil.DAYS.indexOf(state.today).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { TimeUtil.DAYS.size }

    LaunchedEffect(pagerState.currentPage) {
        vm.selectDay(TimeUtil.DAYS[pagerState.currentPage])
    }

    fun saveEvent(ev: CustomEvent) {
        scope.launch {
            val conflicts = repo.checkEventConflict(state.selectedProgramme, ev.dayOfWeek, ev.startTime, ev.endTime)
            if (conflicts.isNotEmpty() && pendingConflict == null) {
                pendingConflict = ev
                return@launch
            }
            repo.saveEvent(ev)
            ReminderScheduler(context).rescheduleAllAsync()
            editingEvent = null
            pendingConflict = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingEvent = CustomEvent(dayOfWeek = state.selectedDay, startTime = "08:00")
            }) { Icon(Icons.Default.Add, contentDescription = "Add event") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Your Schedule", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(state.selectedProgramme, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { vm.syncSchedule() }, enabled = !state.isSyncing) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isSyncing) "Syncing" else "Sync")
                }
            }

            DaySelector(
                selectedDay = state.selectedDay,
                today = state.today,
                onDaySelected = { day ->
                    val idx = TimeUtil.DAYS.indexOf(day)
                    scope.launch { pagerState.animateScrollToPage(idx) }
                },
                onDayLongPress = { day ->
                    editingEvent = CustomEvent(dayOfWeek = day, startTime = "08:00")
                }
            )
            Spacer(Modifier.height(8.dp))

            HorizontalPager(state = pagerState) { page ->
                val day = TimeUtil.DAYS[page]
                val lectures = state.allEntries.filter { it.dayOfWeek == day }.map { DayItem.Lecture(it) }
                val evs = state.allEvents.filter { it.dayOfWeek == day }.map { DayItem.Event(it) }
                val items = (lectures + evs).sortedBy {
                    TimeUtil.toMinutes(when (it) {
                        is DayItem.Lecture -> it.entry.startTime
                        is DayItem.Event -> it.event.startTime
                    })
                }
                DayTimeline(
                    items = items,
                    onLecture = { detailEntry = it },
                    onEvent = { editingEvent = it }
                )
            }
        }
    }

    detailEntry?.let { entry ->
        LectureDetailSheet(
            entry = entry,
            noteText = "",
            onNoteChange = {},
            onAlarmToggle = {},
            selectedAlarmMinutes = null
        )
    }

    editingEvent?.let { ev ->
        EventSheet(
            initial = ev,
            onDismiss = { editingEvent = null },
            onConfirm = { saveEvent(it) },
            onDelete = { id ->
                scope.launch { repo.deleteEvent(id); ReminderScheduler(context).rescheduleAllAsync() }
                editingEvent = null
            }
        )
    }

    pendingConflict?.let { ev ->
        AlertDialog(
            onDismissRequest = { pendingConflict = null },
            title = { Text("Schedule conflict") },
            text = { Text("This event overlaps a class on ${ev.dayOfWeek}. Save anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.saveEvent(ev)
                        ReminderScheduler(context).rescheduleAllAsync()
                        editingEvent = null
                        pendingConflict = null
                    }
                }) { Text("Save anyway") }
            },
            dismissButton = { TextButton(onClick = { pendingConflict = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DaySelector(
    selectedDay: String,
    today: String,
    onDaySelected: (String) -> Unit,
    onDayLongPress: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(TimeUtil.DAYS) { day ->
            val isSelected = day == selectedDay
            val isToday = day == today
            Surface(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { onDaySelected(day) },
                        onLongClick = { onDayLongPress(day) }
                    ),
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isToday && !isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.take(3), style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    val idx = TimeUtil.DAYS.indexOf(day)
                    val dateNum = run {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_MONTH, (idx - (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 7) % 7)
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    }
                    Text("$dateNum", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)