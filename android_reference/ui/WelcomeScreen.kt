package com.must.timetable.features.onboarding.ui

/**
 * Mirrors src/pages/Welcome.jsx:
 * indigo gradient, decorative book icons, stacked blue "MUST" bars,
 * rounded bottom card with branding, "Start Learning Today" CTA and feature icons.
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.timetable.AppGraph

@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val start = {
        AppGraph.repository(context).welcomed = true
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE0E7FF), Color(0xFFEEF2FF)))) // indigo-100 -> indigo-50
    ) {
        // decorative book icons (web: absolute positioned BookOpen icons)
        Icon(
            Icons.Default.MenuBook, null,
            tint = Color(0xFFC7D2FE).copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 64.dp, start = 40.dp)
                .size(36.dp)
        )
        Icon(
            Icons.Default.MenuBook, null,
            tint = Color(0xFFC7D2FE).copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 200.dp, end = 48.dp)
                .size(28.dp)
        )

        // stacked blue bars forming the MUST logo mark
        Box(Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogoBar(160.dp, Color(0xFF1D4ED8), "MUST") // blue-700
                LogoBar(176.dp, Color(0xFF2563EB))        // blue-600
                LogoBar(160.dp, Color(0xFF3B82F6))        // blue-500
                LogoBar(144.dp, Color(0xFF60A5FA))        // blue-400
            }
        }

        // bottom card (rounded-t-3xl)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 40.dp)
            ) {
                // branding row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange, null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        "MUSTimetable",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Start Learning Today", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Never miss a lecture. View your MUST timetable, set class alarms, and take notes — all offline.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = start,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Get Started", fontSize = 16.sp) }
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E7FF)) // indigo-100
                            .clickable { start() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color(0xFF4338CA)) // indigo-700
                    }
                }
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    Feature(Icons.Default.Alarm, "Alarms", Color(0xFFE0E7FF), Color(0xFF4338CA))
                    Feature(Icons.Default.Notes, "Notes", Color(0xFFDCFCE7), Color(0xFF15803D))
                    Feature(Icons.Default.WifiOff, "Offline", Color(0xFFFFEDD5), Color(0xFFC2410C))
                }
            }
        }
    }
}

@Composable
private fun LogoBar(width: androidx.compose.ui.unit.Dp, color: Color, text: String? = null) {
    Box(
        Modifier
            .width(width)
            .height(32.dp)
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, label: String, bg: Color, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}