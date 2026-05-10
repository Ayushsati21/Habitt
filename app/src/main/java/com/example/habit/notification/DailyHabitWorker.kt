package com.example.habit.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.habit.HabitApplication
import com.example.habit.data.HabitLibrary
import com.example.habit.domain.DailyHabitSelector
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that runs once per day to:
 *  1. Pick today's habit using the 60-day no-repeat selector
 *  2. Store it in daily_habits table
 *  3. Fire the lock-screen notification with the two action buttons
 *
 * Schedule: daily, repeating every 24 hours.
 * The notification time is set at scheduling time (see scheduleDaily).
 */
class DailyHabitWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app  = applicationContext as HabitApplication
        val repo = app.repository

        val today    = LocalDate.now()
        val epochDay = today.toEpochDay()

        // If we already showed a habit today, don't show again
        val existing = repo.getDailyHabitForDay(epochDay)
        if (existing != null) return Result.success()

        // Gather recent + tracked to enforce 60-day no-repeat and exclude active quit habits
        val recentIds      = repo.recentDailyHabitIds()
        val trackedLibIds  = repo.trackedHabits.first()
            .mapNotNull { it.libraryHabitId }
            .toSet()

        val chosen = DailyHabitSelector.pick(recentIds, trackedLibIds)
            ?: HabitLibrary.all.randomOrNull()
            ?: return Result.failure()

        // Persist the choice
        repo.insertDailyHabit(epochDay, chosen.id)

        // Fire the lock-screen notification
        NotificationHelper.showDailyHabitNotification(
            context     = applicationContext,
            epochDay    = epochDay,
            libraryId   = chosen.id,
            habitName   = chosen.name,
            stat        = chosen.keyStat,
            contextLine = chosen.shortDescription,
        )

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "habit_daily_notification"

        /**
         * Call once from HabitApplication.onCreate().
         * Schedules a repeating daily worker. On first run it fires immediately,
         * then repeats every 24 hours.
         *
         * For precise user-set time delivery, pair this with AlarmManager
         * to trigger the WorkManager job at the exact hour the user chose.
         */
        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyHabitWorker>(
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
