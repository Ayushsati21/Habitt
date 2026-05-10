package com.example.habit.domain

import com.example.habit.data.model.CheckInOutcome

object Gamification {

    // ── Check-in XP (quit challenge daily check-in) ───────────────────────
    fun xpForDailyCheckIn(outcome: CheckInOutcome): Int = when (outcome) {
        CheckInOutcome.Clean -> 10
        CheckInOutcome.Partial -> 5
        CheckInOutcome.Slip -> 0
    }

    // ── Milestone bonuses ─────────────────────────────────────────────────
    fun xpSevenDayMilestone(): Int = 25
    fun xpHabitBeaten(): Int = 100

    // ── AI scan bonuses ───────────────────────────────────────────────────
    fun xpFirstAiScan(): Int = 20
    fun xpWeeklyRescan(): Int = 10

    // ── Daily notification response XP (PRD: 10/20/30 by severity) ───────
    /**
     * Called when user taps "I avoided it today" on the daily lock-screen notification.
     * @param severity  1 = low, 2 = medium, 3 = high
     */
    fun xpForDailyNotificationAvoided(severity: Int): Int = when (severity) {
        1 -> 10
        2 -> 20
        3 -> 30
        else -> 10
    }

    // ── Bonus XP events ───────────────────────────────────────────────────
    fun xpFirstEverResponse(): Int = 25
    fun xpFirstQuitChallengeStarted(): Int = 50
    fun xpSevenDayAwarenessStreak(): Int = 100
    fun xpShareMilestone(): Int = 20
    fun xpFiveQuitChallengesComplete(): Int = 500
    fun xpComebackBonus(): Int = 20    // after streak reset recovery

    // ── Level system (based on total XP) ─────────────────────────────────
    /**
     * PRD levels:
     * 1 Unaware    0–199
     * 2 Noticing   200–499
     * 3 Aware      500–999
     * 4 Conscious  1,000–2,499
     * 5 Disciplined 2,500–4,999
     * 6 Liberated  5,000+
     */
    fun levelFromXp(totalXp: Int): Int = when {
        totalXp >= 5000 -> 6
        totalXp >= 2500 -> 5
        totalXp >= 1000 -> 4
        totalXp >= 500  -> 3
        totalXp >= 200  -> 2
        else            -> 1
    }

    fun levelLabel(level: Int): String = when (level) {
        1 -> "Unaware"
        2 -> "Noticing"
        3 -> "Aware"
        4 -> "Conscious"
        5 -> "Disciplined"
        6 -> "Liberated"
        else -> "Unaware"
    }

    fun levelLabelFromXp(totalXp: Int): String = levelLabel(levelFromXp(totalXp))

    /** XP required to reach the next level (for progress bar). Returns null at max level. */
    fun nextLevelXp(totalXp: Int): Int? = when {
        totalXp >= 5000 -> null
        totalXp >= 2500 -> 5000
        totalXp >= 1000 -> 2500
        totalXp >= 500  -> 1000
        totalXp >= 200  -> 500
        else            -> 200
    }

    // ── Rank tiers (based on habits beaten) ──────────────────────────────
    fun rankTierLabel(habitsBeaten: Int): String = when {
        habitsBeaten <= 0    -> "Novice"
        habitsBeaten == 1    -> "Aware"
        habitsBeaten == 2    -> "Committed"
        habitsBeaten in 3..4 -> "Disciplined"
        habitsBeaten in 5..6 -> "Elite"
        else                 -> "Liberated"
    }

    // ── Streak freeze tokens ──────────────────────────────────────────────
    /** Earn one freeze token every 7 unbroken awareness streak days. */
    fun shouldEarnFreezeToken(awarenessStreak: Int): Boolean =
        awarenessStreak > 0 && awarenessStreak % 7 == 0

    // ── Leaderboard score ─────────────────────────────────────────────────
    /**
     * PRD: Score = (Habits Beaten × 1000) + (Current Streak Days × 10)
     *            + (Total XP × 0.1) + (Habits Attempted × 50)
     */
    fun leaderboardScore(
        habitsBeaten: Int,
        bestStreak: Int,
        totalXp: Int,
        habitsAttempted: Int,
    ): Long {
        val xpPart = (totalXp * 0.1).toLong()
        return habitsBeaten * 1000L + bestStreak * 10L + xpPart + habitsAttempted * 50L
    }
}
