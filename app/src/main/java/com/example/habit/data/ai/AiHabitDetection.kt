package com.example.habit.data.ai

import com.example.habit.data.model.HabitCategory

data class AiHabitDetection(
    val title: String,
    val category: HabitCategory,
    val severity: Int,
    val personalisedHarm: String,
    val quitBenefit: String,
    val confidence: Float,
    val libraryMatchId: String?,
)
