package com.example.habit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habit.HabitApplication
import com.example.habit.domain.Gamification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles taps on the "I avoided it today" and "I do this" action buttons
 * on the daily lock-screen notification.
 *
 * Registered in AndroidManifest.xml as a receiver with
 * action "com.example.habit.DAILY_RESPONSE".
 *
 * Flow:
 *  AVOIDED    → log response, award XP (10/20/30 by severity), update awareness streak
 *  I_DO_THIS  → log response, auto-start a quit challenge for this habit
 */
class NotificationResponseReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val epochDay  = intent.getLongExtra(NotificationHelper.EXTRA_EPOCH_DAY, -1L)
        val libraryId = intent.getStringExtra(NotificationHelper.EXTRA_LIBRARY_ID) ?: return
        val response  = intent.getStringExtra(NotificationHelper.EXTRA_RESPONSE) ?: return

        if (epochDay < 0) return

        val app = context.applicationContext as HabitApplication
        val repo = app.repository

        scope.launch {
            repo.recordDailyNotificationResponse(
                epochDay  = epochDay,
                libraryId = libraryId,
                response  = response,
            )
        }

        // Dismiss the notification after the user taps
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancel(NotificationHelper.NOTIF_ID_DAILY)
    }
}
