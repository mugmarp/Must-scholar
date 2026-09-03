package com.must.timetable.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Mirrors src/lib/theme.jsx — global theming with Dark Mode + 4 accent colors.
 * Accent HSL values match the web ACCENTS map exactly.
 */
enum class Accent(val label: String, val color: Color) {
    Indigo("Indigo", Color(0xFF4F46E5)),
    Purple("Purple", Color(0xFF6D28D9)),
    Green("Green", Color(0xFF059669)),
    Orange("Orange", Color(0xFFF97316))
}

/** Light scheme mirrors the web :root tokens in src/index.css. */
private val LightScheme = lightColorScheme(
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF737373),
    outline = Color(0xFFE5E5E5),
    primaryContainer = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF0A0A0A)
)

/** Dark scheme mirrors the web .dark tokens in src/index.css. */
private val DarkScheme = darkColorScheme(
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF262626),
    primaryContainer = Color(0xFF0A0A0A),
    onPrimaryContainer = Color(0xFFFAFAFA)
)

@Composable
fun AppTheme(
    dark: Boolean = isSystemInDarkTheme(),
    accent: Accent = Accent.Indigo,
    content: @Composable () -> Unit
) {
    val base = if (dark) DarkScheme else LightScheme
    val scheme = base.copy(
        primary = accent.color,
        onPrimary = Color.White
    )
    MaterialTheme(colorScheme = scheme, content = content)
}