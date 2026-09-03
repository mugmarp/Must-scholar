package com.must.timetable.features.timetable.ui

/**
 * Mirrors src/components/timetable/NextUpCard.jsx
 * Gradient card (accent -> violet-700), "NEXT UP" pill, countdown,
 * session label pill, big title, meta rows and a white chevron circle.
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.timetable.features.timetable.domain.TimetableEntry

@Composable
fun NextUpCard(entry: TimetableEntry, minutesUntil: Int, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val white90 = Color.White.copy(alpha = 0.9f)
    val pill = Color.White.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(accent, Color(0xFF6D28D9))))
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(20.dp)) {
            // top row: NEXT UP pill + countdown
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    "NEXT UP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pill)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Text(
                    formatCountdown(minutesUntil),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(10.dp))
            // course code + session label pill
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.courseCode, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = white90)
                Text(
                    sessionStyle(entry).label,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pill)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(entry.courseTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            // meta: time + room
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaWhite(Icons.Default.Schedule, "${entry.startTime} – ${entry.endTime}")
                if (entry.room.isNotEmpty()) MetaWhite(Icons.Default.LocationOn, entry.room)
            }
            Spacer(Modifier.height(16.dp))
            // bottom: lecturer + chevron circle
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (entry.lecturer.isNotEmpty()) {
                        Icon(Icons.Default.Person, null, tint = white90, modifier = Modifier.size(16.dp))
                        Text(entry.lecturer.split(" ").first(), fontSize = 14.sp, color = white90)
                    }
                }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaWhite(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
        Text(text, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
    }
}