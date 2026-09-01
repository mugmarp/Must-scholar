package com.must.timetable.features.timetable.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Assignments / deadlines / to-dos with an adjustable reminder before the due time.
 */
@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseCode: String? = null,
    val dueAtMillis: Long? = null,
    val reminderMinutes: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val notes: String = "",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val reminderKey: String get() = "assign:$id"
}

enum class Priority { LOW, MEDIUM, HIGH }