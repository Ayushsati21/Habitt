package com.example.habit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habit.data.local.entity.TrackedHabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedHabitDao {
    @Query("SELECT * FROM tracked_habits ORDER BY severity DESC, createdAtEpochMs ASC")
    fun observeAll(): Flow<List<TrackedHabitEntity>>

    @Query("SELECT * FROM tracked_habits WHERE id = :id")
    suspend fun getById(id: String): TrackedHabitEntity?

    @Query("SELECT * FROM tracked_habits WHERE libraryHabitId = :libraryId LIMIT 1")
    suspend fun getByLibraryId(libraryId: String): TrackedHabitEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TrackedHabitEntity)

    @Update
    suspend fun update(entity: TrackedHabitEntity)

    @Query("DELETE FROM tracked_habits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM tracked_habits")
    suspend fun count(): Int
}
