package com.example.habit.ui.util

import com.example.habit.data.HabitLibrary
import com.example.habit.data.local.entity.TrackedHabitEntity

fun TrackedHabitEntity.displayTitle(): String {
    customTitle?.takeIf { it.isNotBlank() }?.let { return it }
    val lib = libraryHabitId?.let { HabitLibrary.byId(it) }
    return lib?.name ?: "Custom habit"
}

fun TrackedHabitEntity.displaySubtitle(): String {
    return category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
