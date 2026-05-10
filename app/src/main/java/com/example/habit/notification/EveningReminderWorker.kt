package com.example.habit.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.habit.HabitApplication
import com.example.habit.data.HabitLibrary
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Fires at ~9 PM every day. If the user has not responded to today's
 * daily habit notification, sends the evening reminder.
 *
 * PRD: "If user does not respond by 9 PM, a soft evening reminder fires:
 * 'Did you avoid today's habit? Log it before midnight to keep your streak.'"
 */
class EveningReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app      = applicationContext as HabitApplication
        val repo     = app.repository
        val epochDay = LocalDate.now().toEpochDay()

        val daily = repo.getDailyHabitForDay(epochDay) ?: return Result.success()

        // Already responded — no reminder needed
        if (daily.response != null) return Result.success()

        // Evening reminder already sent today
        if (daily.eveningReminderFiredMs > 0L) return Result.success()

        val lib = HabitLibrary.byId(daily.libraryHabitId) ?: return Result.success()

        NotificationHelper.showEveningReminder(
            context    = applicationContext,
            epochDay   = epochDay,
            libraryId  = daily.libraryHabitId,
            habitName  = lib.name,
        )

        repo.markEveningReminderSent(epochDay)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "habit_evening_reminder"

        fun scheduleEvening(context: Context) {
            val request = PeriodicWorkRequestBuilder<EveningReminderWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
