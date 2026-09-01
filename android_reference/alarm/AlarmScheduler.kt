package com.must.timetable.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.must.timetable.core.util.TimeUtil
import com.must.timetable.features.timetable.domain.Assignment
import com.must.timetable.features.timetable.domain.CustomEvent

/**
 * Unified reminder scheduler for classes (per-note alarm), custom events and
 * assignment deadlines. Uses exact, Doze-resilient alarms; weekly items
 * re-arm themselves for the next week when they fire.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleClassReminder(
        naturalKey: String, title: String, room: String, startTime: String, dayOfWeek: String, minutesBefore: Int
    ) {
        val triggerAt = TimeUtil.nextWeeklyOccurrenceMillis(dayOfWeek, startTime, minutesBefore)
        scheduleReminder(
            requestKey = "class:$naturalKey",
            title = title,
            body = "Starts at $startTime in $room",
            triggerAt = triggerAt,
            repeatWeekly = true,
            day = dayOfWeek,
            time = startTime,
            lead = minutesBefore
        )
    }

    fun scheduleEventReminder(event: CustomEvent) {
        val lead = event.alarmMinutes ?: return
        val triggerAt = TimeUtil.nextWeeklyOccurrenceMillis(event.dayOfWeek, event.startTime, lead)
        scheduleReminder(
            requestKey = event.reminderKey,
            title = event.title,
            body = "${event.location ?: "Personal event"} • ${event.startTime}",
            triggerAt = triggerAt,
            repeatWeekly = event.repeatWeekly,
            day = event.dayOfWeek,
            time = event.startTime,
            lead = lead
        )
    }

    fun scheduleAssignmentReminder(assignment: Assignment) {
        val lead = assignment.reminderMinutes ?: return
        val due = assignment.dueAtMillis ?: return
        val triggerAt = due - lead * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return
        scheduleReminder(
            requestKey = assignment.reminderKey,
            title = assignment.title,
            body = "Assignment due soon",
            triggerAt = triggerAt,
            repeatWeekly = false,
            day = null,
            time = null,
            lead = lead
        )
    }

    fun cancelReminder(requestKey: String) {
        val intent = Intent(context, ClassAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, requestKey.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
    }

    fun rearmWeekly(
        requestKey: String, title: String, body: String,
        day: String, time: String, lead: Int, triggerAt: Long
    ) {
        scheduleReminder(requestKey, title, body, triggerAt, true, day, time, lead)
    }

    private fun scheduleReminder(
        requestKey: String, title: String, body: String, triggerAt: Long,
        repeatWeekly: Boolean, day: String?, time: String?, lead: Int
    ) {
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_REPEAT, repeatWeekly)
            putExtra(EXTRA_DAY, day)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_LEAD, lead)
            putExtra(EXTRA_KEY, requestKey)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestKey.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_REPEAT = "extra_repeat"
        const val EXTRA_DAY = "extra_day"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_LEAD = "extra_lead"
        const val EXTRA_KEY = "extra_key"
    }
}