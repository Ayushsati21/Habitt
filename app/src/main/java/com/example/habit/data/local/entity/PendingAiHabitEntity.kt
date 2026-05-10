package com.example.habit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_ai_habits")
data class PendingAiHabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val severity: Int,
    val personalisedHarm: String,
    val quitBenefit: String,
    val confidence: Float,
    val libraryMatchId: String?,
    val createdAtEpochMs: Long,
    val accepted: Boolean?,
)
