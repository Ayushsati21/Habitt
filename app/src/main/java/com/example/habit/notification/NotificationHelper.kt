package com.example.habit.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.habit.MainActivity
import com.example.habit.R

object NotificationHelper {

    const val CHANNEL_DAILY       = "habit_daily"
    const val CHANNEL_REMINDER    = "habit_reminder"
    const val CHANNEL_CHALLENGE   = "habit_challenge"

    const val NOTIF_ID_DAILY      = 1001
    const val NOTIF_ID_EVENING    = 1002
    const val NOTIF_ID_CHALLENGE  = 1003

    // Intent extras
    const val EXTRA_EPOCH_DAY     = "epoch_day"
    const val EXTRA_LIBRARY_ID    = "library_id"
    const val EXTRA_RESPONSE      = "response"

    const val RESPONSE_AVOIDED    = "AVOIDED"
    const val RESPONSE_I_DO_THIS  = "I_DO_THIS"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY,
                "Daily habit",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "One surprising bad habit shown every morning on your lock screen."
                setShowBadge(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDER,
                "Evening reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Gentle evening nudge if you haven't responded to today's habit."
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHALLENGE,
                "Quit challenge",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily check-in for active quit challenges."
            }
        )
    }

    /**
     * Builds and posts the daily lock-screen notification.
     *
     * Layout (PRD spec):
     *   Title    → habit name
     *   BigText  → one-line stat + one-line context
     *   Action 1 → "I avoided it today"  (AVOIDED)
     *   Action 2 → "I do this"           (I_DO_THIS)
     */
    fun showDailyHabitNotification(
        context: Context,
        epochDay: Long,
        libraryId: String,
        habitName: String,
        stat: String,
        contextLine: String,
    ) {
        val openIntent = mainPendingIntent(context, epochDay, libraryId)

        val avoidedIntent = responsePendingIntent(
            context, epochDay, libraryId, RESPONSE_AVOIDED, requestCode = 101
        )
        val iDoThisIntent = responsePendingIntent(
            context, epochDay, libraryId, RESPONSE_I_DO_THIS, requestCode = 102
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(habitName)
            .setContentText(stat)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$stat\n\n$contextLine")
            )
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // show on lock screen
            .addAction(0, "✅  I avoided it today", avoidedIntent)
            .addAction(0, "⚠️  I do this", iDoThisIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_DAILY, notification)
        android.util.Log.d("HabitNotif", "Daily notification posted for: $habitName")
    }

    /**
     * Evening reminder if user hasn't responded by 9 PM.
     */
    fun showEveningReminder(
        context: Context,
        epochDay: Long,
        libraryId: String,
        habitName: String,
    ) {
        val openIntent = mainPendingIntent(context, epochDay, libraryId)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Did you avoid today's habit?")
            .setContentText("$habitName — log it before midnight to keep your streak.")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_EVENING, notification)
    }

    /**
     * Challenge check-in notification for active quit challenges.
     */
    fun showChallengeCheckIn(
        context: Context,
        habitName: String,
        streakDay: Int,
    ) {
        val openIntent = mainPendingIntent(context, 0L, "")

        val notification = NotificationCompat.Builder(context, CHANNEL_CHALLENGE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Day $streakDay of quitting $habitName")
            .setContentText("Tap to log today's check-in and keep your streak alive.")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_CHALLENGE, notification)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun mainPendingIntent(context: Context, epochDay: Long, libraryId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_EPOCH_DAY, epochDay)
            putExtra(EXTRA_LIBRARY_ID, libraryId)
        }
        return PendingIntent.getActivity(
            context, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun responsePendingIntent(
        context: Context,
        epochDay: Long,
        libraryId: String,
        response: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationResponseReceiver::class.java).apply {
            action = "com.example.habit.DAILY_RESPONSE"
            putExtra(EXTRA_EPOCH_DAY, epochDay)
            putExtra(EXTRA_LIBRARY_ID, libraryId)
            putExtra(EXTRA_RESPONSE, response)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
