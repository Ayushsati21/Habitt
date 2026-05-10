package com.example.habit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habit.data.local.entity.PendingAiHabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAiHabitDao {
    @Query("SELECT * FROM pending_ai_habits WHERE accepted IS NULL ORDER BY severity DESC, confidence DESC")
    fun observePending(): Flow<List<PendingAiHabitEntity>>

    @Query("SELECT * FROM pending_ai_habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingAiHabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PendingAiHabitEntity>)

    @Query("DELETE FROM pending_ai_habits WHERE accepted IS NULL")
    suspend fun clearPending()

    @Query("UPDATE pending_ai_habits SET accepted = :accepted WHERE id = :id")
    suspend fun setAccepted(id: String, accepted: Boolean)
}
