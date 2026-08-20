package com.must.timetable.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.must.timetable.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val scheduler = AlarmScheduler(context)
                    // In production: load saved programme from DataStore,
                    // fetch all its entries from Room, and re-arm alarms.
                    // scheduler.rescheduleAllAlarms(entries)
                }
            }
        }
    }
}