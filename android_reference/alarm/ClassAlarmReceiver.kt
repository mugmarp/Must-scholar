package com.must.timetable.core.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ClassAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val courseTitle = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_TITLE) ?: "Class"
        val room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""
        val startTime = intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME) ?: ""
        showNotification(context, courseTitle, room, startTime)
    }

    private fun showNotification(
        context: Context, title: String, room: String, startTime: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Class Reminders", NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upcoming: $title")
            .setContentText("Starts at $startTime in $room")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "must_class_reminders"
    }
}