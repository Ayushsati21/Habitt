package com.example.habit

import android.app.Application
import com.example.habit.data.HabitRepository
import com.example.habit.data.local.HabitDatabase
import com.example.habit.notification.DailyHabitWorker
import com.example.habit.notification.EveningReminderWorker
import com.example.habit.notification.NotificationHelper

class HabitApplication : Application() {
    lateinit var repository: HabitRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Database + repository
        val db = HabitDatabase.getInstance(this)
        repository = HabitRepository(db, BuildConfig.CLAUDE_API_KEY)

        // 2. Create notification channels (must run before any notification fires)
        NotificationHelper.createChannels(this)

        // 3. Schedule the daily morning notification worker (24-hour repeat)
        DailyHabitWorker.scheduleDaily(this)

        // 4. Schedule the evening reminder worker (fires ~9 PM if user hasn't responded)
        EveningReminderWorker.scheduleEvening(this)
    }
}
