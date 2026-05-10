package com.example.habit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.habit.data.local.dao.CheckInDao
import com.example.habit.data.local.dao.DailyHabitDao
import com.example.habit.data.local.dao.PendingAiHabitDao
import com.example.habit.data.local.dao.TrackedHabitDao
import com.example.habit.data.local.dao.UserProfileDao
import com.example.habit.data.local.entity.CheckInEntity
import com.example.habit.data.local.entity.DailyHabitEntity
import com.example.habit.data.local.entity.PendingAiHabitEntity
import com.example.habit.data.local.entity.TrackedHabitEntity
import com.example.habit.data.local.entity.UserProfileEntity

@Database(
    entities = [
        TrackedHabitEntity::class,
        CheckInEntity::class,
        UserProfileEntity::class,
        PendingAiHabitEntity::class,
        DailyHabitEntity::class,      // ← NEW: daily notification habit
    ],
    version = 3,                       // ← bumped from 2 → 3
    exportSchema = false,
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun trackedHabitDao(): TrackedHabitDao
    abstract fun checkInDao(): CheckInDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun pendingAiHabitDao(): PendingAiHabitDao
    abstract fun dailyHabitDao(): DailyHabitDao   // ← NEW

    companion object {
        @Volatile
        private var instance: HabitDatabase? = null

        // Existing migration: v1 → v2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracked_habits ADD COLUMN lastAiInsight TEXT")
                db.execSQL("ALTER TABLE tracked_habits ADD COLUMN lastAiScanEpochMs INTEGER")
            }
        }

        // New migration: v2 → v3 — adds daily_habits table + streakFreezeTokens to user_profile
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // daily_habits table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_habits (
                        epochDay INTEGER NOT NULL PRIMARY KEY,
                        libraryHabitId TEXT NOT NULL,
                        response TEXT,
                        xpEarned INTEGER NOT NULL DEFAULT 0,
                        notificationFiredMs INTEGER NOT NULL DEFAULT 0,
                        eveningReminderFiredMs INTEGER NOT NULL DEFAULT 0,
                        createdAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // streakFreezeTokens column on user_profile
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN streakFreezeTokens INTEGER NOT NULL DEFAULT 2"
                )
                // level column on user_profile (derived but cached for display)
                db.execSQL(
                    "ALTER TABLE user_profile ADD COLUMN currentLevel INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun getInstance(context: Context): HabitDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
