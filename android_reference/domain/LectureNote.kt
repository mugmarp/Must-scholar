package com.must.timetable.features.timetable.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Notes are linked via the Business Natural Key, NOT the Room row ID.
 * When a new timetable draft syncs and the TimetableEntry row is replaced
 * (UPSERT), the note persists because the natural key is unchanged.
 */
@Entity(tableName = "lecture_notes")
data class LectureNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val naturalKey: String,
    val content: String,
    val updatedAt: Long,
    val colourTag: String? = null,
    /** Minutes before class to remind; null = off. */
    val alarmMinutes: Int? = null
)