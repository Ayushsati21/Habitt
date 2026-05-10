package com.example.habit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habit.data.local.entity.DailyHabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyHabitDao {

    /** Today's daily habit card (if already assigned). */
    @Query("SELECT * FROM daily_habits WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getForDay(epochDay: Long): DailyHabitEntity?

    /** Observe today's card so the UI reacts when user responds. */
    @Query("SELECT * FROM daily_habits WHERE epochDay = :epochDay LIMIT 1")
    fun observeForDay(epochDay: Long): Flow<DailyHabitEntity?>

    /** All days — used to enforce the 60-day no-repeat rule. */
    @Query("SELECT libraryHabitId FROM daily_habits ORDER BY epochDay DESC LIMIT 60")
    suspend fun recentHabitIds(): List<String>

    /** Full history for the History screen (newest first). */
    @Query("SELECT * FROM daily_habits ORDER BY epochDay DESC LIMIT 200")
    fun observeHistory(): Flow<List<DailyHabitEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DailyHabitEntity)

    @Update
    suspend fun update(entity: DailyHabitEntity)

    /** Mark response and XP earned. */
    @Query("UPDATE daily_habits SET response = :response, xpEarned = :xp WHERE epochDay = :epochDay")
    suspend fun setResponse(epochDay: Long, response: String, xp: Int)

    /** Mark that the evening reminder was sent. */
    @Query("UPDATE daily_habits SET eveningReminderFiredMs = :ms WHERE epochDay = :epochDay")
    suspend fun setEveningReminderFired(epochDay: Long, ms: Long)
}
