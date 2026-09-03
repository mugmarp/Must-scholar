package com.must.timetable.features.timetable.ui

/**
 * Mirrors src/components/timetable/LectureCard.jsx, EventCard.jsx and the
 * day list in Home.jsx (space-y-3 spacing, empty state copy).
 * Session style colors mirror SESSION_STYLES in src/lib/timetableUtils.js.
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.TimetableEntry

// ---- Session styling: mirrors SESSION_STYLES / sessionStyle() on the web ----

data class SessionStyle(val label: String, val badge: Color, val accent: Color, val text: Color)

internal fun inferSessionType(entry: TimetableEntry): String {
    entry.sessionType?.let { return it.uppercase() }
    val room = entry.room.uppercase()
    val title = entry.courseTitle.uppercase()
    if (room.contains("LAB")) return "PRACTICAL"
    if (title.contains("CLINICAL") || title.contains("WARD")) return "CLINICAL"
    return "THEORY"
}

internal fun sessionStyle(entry: TimetableEntry): SessionStyle = when (inferSessionType(entry)) {
    "PRACTICAL" -> SessionStyle("Practical", Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF16A34A))
    "CLINICAL", "WARD" -> SessionStyle("Clinical", Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFD97706))
    "THEORY" -> SessionStyle("Theory", Color(0xFF2563EB), Color(0xFF3B82F6), Color(0xFF2563EB))
    else -> SessionStyle("Class", Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF64748B))
}

// ---- Day list (mirrors the agenda list in Home.jsx) ----

@Composable
fun DayTimeline(
    items: List<DayItem>,
    onLecture: (TimetableEntry) -> Unit,
    onEvent: (CustomEvent) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
            Text(
                "No classes or events for this day.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // web space-y-3
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

// ---- Lecture card (mirrors LectureCard.jsx: left accent bar, badge, chevron circle) ----

@Composable
fun LectureCard(entry: TimetableEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = sessionStyle(entry)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // left accent bar
            Box(Modifier.width(4.dp).fillMaxHeight().background(style.accent))
            Column(Modifier.padding(16.dp).padding(start = 8.dp)) {
                // course code + session badge
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.MenuBook, null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text(entry.courseCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = style.text)
                    }
                    Text(
                        style.label,
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(style.badge)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    entry.courseTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                // time + room meta
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text("${entry.startTime}–${entry.endTime}", fontSize = 12.sp, color = onSurfaceVariant)
                    }
                    if (entry.room.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text(entry.room, fontSize = 12.sp, color = onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // lecturer + shared count + chevron circle
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (entry.lecturer.isNotEmpty()) {
                            Icon(Icons.Default.Person, null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text(entry.lecturer.split(" ").first(), fontSize = 12.sp, color = onSurfaceVariant)
                        }
                        if (entry.sharedGroupsList.size > 1) {
                            Text("+${entry.sharedGroupsList.size - 1}", fontSize = 12.sp, color = onSurfaceVariant)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---- Event card (mirrors EventCard.jsx: purple dashed-border personal card) ----

@Composable
fun EventCard(event: CustomEvent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Web uses purple-50/purple-300 in light mode, purple-950/30 + purple-700 in dark mode.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (dark) Color(0xFF581C87).copy(alpha = 0.3f) else Color(0xFFFAF5FF)
    val cardBorder = if (dark) Color(0xFF6B21A8) else Color(0xFFD8B4FE)
    val labelColor = Color(0xFF9333EA) // purple-600
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        // NOTE: web uses a dashed border; Compose borders are solid — closest visual match.
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(Color(0xFFA855F7))) // purple-500
            Column(Modifier.padding(16.dp).padding(start = 8.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Event, null, tint = labelColor, modifier = Modifier.size(14.dp))
                        Text("PERSONAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = labelColor)
                    }
                    if (event.alarmMinutes != null) {
                        Icon(Icons.Default.Notifications, null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    event.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text(
                            event.startTime + (event.endTime?.let { "–$it" } ?: ""),
                            fontSize = 12.sp,
                            color = onSurfaceVariant
                        )
                    }
                    if (!event.location.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text(event.location, fontSize = 12.sp, color = onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9333EA).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = labelColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}