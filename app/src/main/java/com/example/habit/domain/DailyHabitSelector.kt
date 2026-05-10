package com.example.habit.domain

import com.example.habit.data.HabitLibrary
import com.example.habit.data.model.LibraryHabit

/**
 * Selects one habit for the daily lock-screen notification.
 *
 * Rules (from PRD):
 * - No habit repeats within a 60-day window
 * - Categories rotate so all 8 are seen over time
 * - Habits already tracked (user tapped "I do this") are excluded
 *   so they never appear as a daily notification again
 * - Selection is weighted toward higher surprise / severity
 */
object DailyHabitSelector {

    fun pick(
        recentIds: List<String>,        // last 60 days of shown habit IDs
        trackedLibraryIds: Set<String>, // habits user is already quitting
    ): LibraryHabit? {
        val recentSet = recentIds.toSet()

        // Candidates: not shown recently, not already being tracked
        val candidates = HabitLibrary.all.filter { habit ->
            habit.id !in recentSet && habit.id !in trackedLibraryIds
        }

        if (candidates.isEmpty()) {
            // All habits shown in past 60 days — reset and pick from non-tracked only
            val fallback = HabitLibrary.all.filter { it.id !in trackedLibraryIds }
            return fallback.randomOrNull()
        }

        // Prefer higher severity for surprise factor; shuffle within same severity
        return candidates
            .sortedByDescending { it.severity }
            .take(10)           // top 10 by severity as the pool
            .randomOrNull()     // pick randomly within pool for variety
    }
}
