package com.example.habit.data

data class AiScanResult(
    val usedCloudAi: Boolean,
    val insightsApplied: Int,
    val newSuggestions: Int,
    val note: String?,
)
