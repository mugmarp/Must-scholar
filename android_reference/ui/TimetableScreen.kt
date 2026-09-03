package com.must.timetable.features.timetable.ui

/**
 * Mirrors src/pages/Home.jsx:
 * header (M avatar, greeting, + button, green Synced pill), H1,
 * programme selector, Next Up card, rounded day pills, swipe pager.
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.timetable.AppGraph
import com.must.timetable.core.alarm.ReminderScheduler
import com.must.timetable.core.util.TimeUtil
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.launch
import java.util.Calendar

// ---- findNextUp / formatCountdown: mirror src/lib/timetableUtils.js ----

internal data class NextUp(val entry: TimetableEntry, val minutesUntil: Int)

internal fun findNextUp(entries: List<TimetableEntry>, nowMinutes: Int, todayIdx: Int): NextUp? {
    var best: NextUp? = null
    for (offset in TimeUtil.DAYS.indices) {
        val day = TimeUtil.DAYS[(todayIdx + offset) % TimeUtil.DAYS.size]
        entries.filter { it.dayOfWeek == day }
            .sortedBy { TimeUtil.toMinutes(it.startTime) }
            .forEach { e ->
                val minutesUntil = TimeUtil.toMinutes(e.startTime) + offset * 1440 - nowMinutes
                if (minutesUntil > 0 && (best == null || minutesUntil < best!!.minutesUntil)) {
                    best = NextUp(e, minutesUntil)
                }
            }
    }
    return best
}

internal fun formatCountdown(minutes: Int): String {
    if (minutes <= 0) return "Starting now"
    val d = minutes / 1440
    val h = (minutes % 1440) / 60
    val m = minutes % 60
    return when {
        d > 0 -> "in ${d}d ${h}h"
        h > 0 -> "in ${h}h ${m}m"
        else -> "in ${m}m"
    }
}

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

    val now = remember { Calendar.getInstance() }
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val todayIdx = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val nextUp = findNextUp(state.allEntries, nowMinutes, todayIdx)
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
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

    Column(Modifier.fillMaxSize()) {
        // ---- Header: avatar, greeting, + button, Synced pill (mirrors Home.jsx) ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(greeting, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        state.selectedProgramme,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // round "+" button (web: no FAB, the + lives in the header)
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { editingEvent = CustomEvent(dayOfWeek = state.selectedDay, startTime = "08:00") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add, "Add event",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // green Synced pill (tap to sync)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFDCFCE7), // green-100
                    modifier = Modifier.clickable(enabled = !state.isSyncing) { vm.syncSchedule() }
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                        Text(
                            if (state.isSyncing) "Syncing" else "Synced",
                            fontSize = 12.sp,
                            color = Color(0xFF15803D) // green-700
                        )
                    }
                }
            }
        }

        Text(
            "Your Schedule Today",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))

        ProgrammeSelector(
            programmes = state.programmes,
            value = state.selectedProgramme,
            onChange = { vm.selectProgramme(it) }
        )
        Spacer(Modifier.height(12.dp))

        nextUp?.let {
            NextUpCard(entry = it.entry, minutesUntil = it.minutesUntil, onClick = { detailEntry = it.entry })
            Spacer(Modifier.height(12.dp))
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

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val day = TimeUtil.DAYS[page]
            val lectures = state.allEntries.filter { it.dayOfWeek == day }.map { DayItem.Lecture(it) }
            val evs = state.allEvents.filter { it.dayOfWeek == day }.map { DayItem.Event(it) }
            val items = (lectures + evs).sortedBy {
                TimeUtil.toMinutes(
                    when (it) {
                        is DayItem.Lecture -> it.entry.startTime
                        is DayItem.Event -> it.event.startTime
                    }
                )
            }
            DayTimeline(
                items = items,
                onLecture = { detailEntry = it },
                onEvent = { editingEvent = it }
            )
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

// ---- Programme selector (mirrors ProgrammeSelector.jsx: School icon + dropdown) ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgrammeSelector(programmes: List<String>, value: String, onChange: (String) -> Unit) {
    if (programmes.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Icon(
            Icons.Default.School, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                programmes.forEach { p ->
                    DropdownMenuItem(text = { Text(p) }, onClick = { onChange(p); expanded = false })
                }
            }
        }
    }
}

// ---- Day selector (mirrors DaySelector.jsx: rounded-2xl pills, today ring) ----

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DaySelector(
    selectedDay: String,
    today: String,
    onDaySelected: (String) -> Unit,
    onDayLongPress: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    val mondayIdx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0 .. Sun=6
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(TimeUtil.DAYS.size) { i ->
            val day = TimeUtil.DAYS[i]
            val isSelected = day == selectedDay
            val isToday = day == today
            val dateNum = cal.get(Calendar.DAY_OF_MONTH) + (i - mondayIdx)
            Surface(
                modifier = Modifier.combinedClickable(
                    onClick = { onDaySelected(day) },
                    onLongClick = { onDayLongPress(day) }
                ),
                shape = RoundedCornerShape(16.dp), // rounded-2xl
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isToday && !isSelected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        day.take(3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$dateNum",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}