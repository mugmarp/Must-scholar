package com.must.timetable.features.timetable.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Personal study events created by the student, shown alongside official
 * timetable entries. Optional weekly recurrence + reminder.
 */
@Entity(tableName = "custom_events")
data class CustomEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String? = null,
    val location: String? = null,
    val notes: String = "",
    val repeatWeekly: Boolean = true,
    val alarmMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val reminderKey: String get() = "event:$id"
}