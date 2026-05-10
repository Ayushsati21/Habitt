package com.example.habit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules the daily habit notification WorkManager jobs
 * after the device restarts (WorkManager persists work but
 * this ensures channels and scheduling are re-applied).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.createChannels(context)
            DailyHabitWorker.scheduleDaily(context)
            EveningReminderWorker.scheduleEvening(context)
        }
    }
}
