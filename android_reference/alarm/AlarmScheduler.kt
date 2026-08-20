package com.must.timetable.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.must.timetable.features.timetable.domain.TimetableEntry
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleClassReminder(entry: TimetableEntry, minutesBefore: Int) {
        val triggerAtMillis = calculateTriggerTime(entry, minutesBefore) ?: return

        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra(EXTRA_NATURAL_KEY, entry.naturalKey)
            putExtra(EXTRA_COURSE_TITLE, entry.courseTitle)
            putExtra(EXTRA_ROOM, entry.room)
            putExtra(EXTRA_START_TIME, entry.startTime)
        }

        val requestCode = entry.naturalKey.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }

    fun cancelAlarm(entry: TimetableEntry) {
        val intent = Intent(context, ClassAlarmReceiver::class.java)
        val requestCode = entry.naturalKey.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    suspend fun rescheduleAllAlarms(entries: List<TimetableEntry>) {
        entries.forEach { entry ->
            val triggerTime = calculateTriggerTime(entry, minutesBefore = 15)
            if (triggerTime != null && triggerTime > System.currentTimeMillis()) {
                scheduleClassReminder(entry, 15)
            }
        }
    }

    private fun calculateTriggerTime(entry: TimetableEntry, minutesBefore: Int): Long? {
        return try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, dayNameToOffset(entry.dayOfWeek))
            val (hour, minute) = parseTime(entry.startTime)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute - minutesBefore)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    private fun dayNameToOffset(day: String): Int = when (day) {
        "Sunday" -> Calendar.SUNDAY
        "Monday" -> Calendar.MONDAY
        "Tuesday" -> Calendar.TUESDAY
        "Wednesday" -> Calendar.WEDNESDAY
        "Thursday" -> Calendar.THURSDAY
        "Friday" -> Calendar.FRIDAY
        "Saturday" -> Calendar.SATURDAY
        else -> Calendar.MONDAY
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        return Pair(parts[0].toInt(), parts.getOrElse(1) { "0" }.toInt())
    }

    companion object {
        const val EXTRA_NATURAL_KEY = "extra_natural_key"
        const val EXTRA_COURSE_TITLE = "extra_course_title"
        const val EXTRA_ROOM = "extra_room"
        const val EXTRA_START_TIME = "extra_start_time"
    }
}