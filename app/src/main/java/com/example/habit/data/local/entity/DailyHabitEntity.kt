package com.example.habit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per calendar day. Tracks which habit the lock-screen notification showed
 * and how the user responded.
 *
 * response values:
 *   null          = not yet responded
 *   "AVOIDED"     = "I avoided it today" → earn XP, log in history
 *   "I_DO_THIS"   = "I do this" → enter quit challenge for this habit
 *   "DISMISSED"   = notification dismissed without response
 */
@Entity(tableName = "daily_habits")
data class DailyHabitEntity(
    @PrimaryKey val epochDay: Long,          // LocalDate.toEpochDay()
    val libraryHabitId: String,              // which habit was shown today
    val response: String? = null,            // null | AVOIDED | I_DO_THIS | DISMISSED
    val xpEarned: Int = 0,
    val notificationFiredMs: Long = 0L,      // when the morning notification fired
    val eveningReminderFiredMs: Long = 0L,   // when the evening reminder fired (0 = not yet)
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
