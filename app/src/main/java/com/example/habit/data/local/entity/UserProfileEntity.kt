package com.example.habit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingComplete: Boolean,
    val totalXp: Int,
    val weeklyXp: Int,
    val weekStartEpochDay: Long,
    val habitsBeaten: Int,
    val firstAiScanDone: Boolean,
    val leaderboardVisibility: String,
    val moneySavedInr: Long,
    // ── NEW fields (migration 2 → 3) ──────────────────────────────────────
    /** Freeze tokens protect the daily awareness streak from a missed day. Earned every 7 clean days. */
    val streakFreezeTokens: Int = 2,
    /** Cached level (1–6) so the UI doesn't recompute every frame. */
    val currentLevel: Int = 1,
)
