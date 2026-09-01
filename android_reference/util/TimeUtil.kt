package com.must.timetable.core.util

import java.util.Calendar

object TimeUtil {
    val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun todayName(): String {
        val cal = Calendar.getInstance()
        val idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0 .. Sun=6
        return DAYS[idx]
    }

    fun toMinutes(time: String?): Int {
        if (time.isNullOrBlank()) return 0
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    fun rangesOverlap(s1: Int, e1: Int, s2: Int, e2: Int): Boolean = s1 < e2 && s2 < e1

    /** Next future trigger time (epoch ms) for a weekly recurring item, offset by lead minutes. */
    fun nextWeeklyOccurrenceMillis(dayName: String, timeStr: String, leadMinutes: Int = 0): Long {
        val cal = Calendar.getInstance()
        val targetMonIdx = DAYS.indexOf(dayName).let { if (it < 0) 0 else it }
        val nowMonIdx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val diff = (targetMonIdx - nowMonIdx + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, diff)
        val (h, m) = parseTime(timeStr)
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m - leadMinutes)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 7)
        }
        return cal.timeInMillis
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
}