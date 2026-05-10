package com.example.habit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habit.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query(
        "SELECT * FROM check_ins WHERE habitId = :habitId ORDER BY epochDay DESC LIMIT :limit",
    )
    fun observeRecentForHabit(habitId: String, limit: Int = 30): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getForDay(habitId: String, epochDay: Long): CheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CheckInEntity)

    @Query("SELECT COUNT(*) FROM check_ins WHERE outcome = 'CLEAN'")
    fun observeCleanCheckInCount(): Flow<Int>

    @Query("SELECT * FROM check_ins ORDER BY epochDay DESC LIMIT 200")
    fun observeRecent(): Flow<List<CheckInEntity>>
}
