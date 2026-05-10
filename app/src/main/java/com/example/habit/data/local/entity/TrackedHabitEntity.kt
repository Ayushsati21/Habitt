package com.example.habit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracked_habits",
    indices = [Index(value = ["libraryHabitId"], unique = false)],
)
data class TrackedHabitEntity(
    @PrimaryKey val id: String,
    val libraryHabitId: String?,
    val customTitle: String?,
    val category: String,
    val severity: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastCleanEpochDay: Long?,
    val isBeaten: Boolean,
    val createdAtEpochMs: Long,
    val isSensitive: Boolean = false,
    /** Latest personalised note from AI scan for habits already tracked (library match). */
    val lastAiInsight: String? = null,
    val lastAiScanEpochMs: Long? = null,
)
