package com.example.habit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_ins",
    indices = [
        Index(value = ["habitId", "epochDay"], unique = true),
    ],
)
data class CheckInEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long,
    val outcome: String,
    val xpEarned: Int,
    val createdAtEpochMs: Long,
)
