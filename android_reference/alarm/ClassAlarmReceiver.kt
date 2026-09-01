package com.must.timetable.core.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Generic reminder receiver. Shows the notification, and for weekly-repeating
 * items re-arms the alarm for the next week.
 */
class ClassAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Reminder"
        val body = intent.getStringExtra(AlarmScheduler.EXTRA_BODY) ?: ""
        showNotification(context, title, body)

        if (intent.getBooleanExtra(AlarmScheduler.EXTRA_REPEAT, false)) {
            val day = intent.getStringExtra(AlarmScheduler.EXTRA_DAY) ?: return
            val time = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: return
            val lead = intent.getIntExtra(AlarmScheduler.EXTRA_LEAD, 0)
            val key = intent.getStringExtra(AlarmScheduler.EXTRA_KEY) ?: return
            val triggerAt = com.must.timetable.core.util.TimeUtil.nextWeeklyOccurrenceMillis(day, time, lead)
            AlarmScheduler(context).rearmWeekly(key, title, body, day, time, lead, triggerAt)
        }
    }

    private fun showNotification(context: Context, title: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ $title")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), n)
    }

    companion object {
        const val CHANNEL_ID = "must_reminders"
    }
}