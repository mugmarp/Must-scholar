package com.must.timetable.core.alarm

import android.content.Context
import com.must.timetable.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms every active reminder (class notes with alarms, custom events,
 * assignments) — called on boot / time-zone change and after data changes.
 */
class ReminderScheduler(private val context: Context) {

    private val db = AppDatabase.get(context)
    private val scheduler = AlarmScheduler(context)

    suspend fun rescheduleAll() {
        val prefs = context.getSharedPreferences("must_prefs", Context.MODE_PRIVATE)
        val programme = prefs.getString("programme", "MBR I") ?: "MBR I"

        // Classes: notes that have an alarm, matched to their timetable entry.
        val notes = db.timetableDao().getNotesWithAlarms()
        val entries = db.timetableDao().getEntriesForProgrammeOnce(programme)
        val byKey = entries.associateBy { it.naturalKey }
        notes.forEach { note ->
            val entry = byKey[note.naturalKey] ?: return@forEach
            val lead = note.alarmMinutes ?: return@forEach
            scheduler.scheduleClassReminder(
                entry.naturalKey, entry.courseTitle, entry.room,
                entry.startTime, entry.dayOfWeek, lead
            )
        }

        // Custom events.
        db.customEventDao().getAllListOnce().forEach { ev ->
            if (ev.alarmMinutes != null) scheduler.scheduleEventReminder(ev)
        }

        // Assignments (not completed, with reminder + due date).
        db.assignmentDao().getAllListOnce().forEach { a ->
            if (!a.completed && a.reminderMinutes != null && a.dueAtMillis != null) {
                scheduler.scheduleAssignmentReminder(a)
            }
        }
    }

    fun rescheduleAllAsync() {
        CoroutineScope(Dispatchers.IO).launch { rescheduleAll() }
    }
}