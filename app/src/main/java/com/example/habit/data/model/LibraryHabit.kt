package com.example.habit.data.model

data class LibraryHabit(
    val id: String,
    val name: String,
    val category: HabitCategory,
    val severity: Int,
    val shortDescription: String,
    val longDescription: String,
    val keyStat: String,
    val annualCostInr: String,
    val quitBenefit: String,
    val xpReward: Int = 10,
)

enum class HabitCategory {
    Diet,
    Digital,
    Substance,
    Sleep,
    Physical,
    Social,
    Mental,
    Financial,
}

enum class CheckInOutcome {
    Clean,
    Partial,
    Slip,
}
