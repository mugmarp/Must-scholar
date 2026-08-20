package com.must.timetable.features.timetable.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The composite Business Natural Key ensures that notes and alarms
 * survive timetable draft updates. Even if the backend UUID changes
 * between Draft 1 -> Draft 2 -> Final, the natural key stays stable
 * so user notes are never orphaned.
 *
 * Key = programme_group + course_code + day_of_week + start_time
 */
@Entity(
    tableName = "timetable_entries",
    indices = [
        Index(
            value = ["programmeGroup", "courseCode", "dayOfWeek", "startTime"],
            unique = true
        )
    ]
)
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val programmeGroup: String,
    val courseCode: String,
    val dayOfWeek: String,
    val startTime: String,

    val endTime: String,
    val courseTitle: String,
    val sessionType: String?,
    val lecturer: String,
    val room: String,
    val timeSlot: String,
    val sharedWithRaw: String,

    val draftVersion: String,
    val lastSyncedAt: Long
) {
    val naturalKey: String
        get() = "${programmeGroup}|${courseCode}|${dayOfWeek}|${startTime}"

    val sharedGroupsList: List<String>
        get() = sharedWithRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}