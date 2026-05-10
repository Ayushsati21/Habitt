package com.example.habit.data

import com.example.habit.data.ai.ClaudeHabitAnalyzer
import com.example.habit.data.ai.LocalRoutineAnalyzer
import com.example.habit.data.local.HabitDatabase
import com.example.habit.data.local.entity.CheckInEntity
import com.example.habit.data.local.entity.DailyHabitEntity
import com.example.habit.data.local.entity.PendingAiHabitEntity
import com.example.habit.data.local.entity.TrackedHabitEntity
import com.example.habit.data.local.entity.UserProfileEntity
import com.example.habit.data.model.CheckInOutcome
import com.example.habit.data.model.HabitCategory
import com.example.habit.domain.Gamification
import com.example.habit.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

class HabitRepository(
    private val db: HabitDatabase,
    claudeApiKey: String,
) {
    private val trackedDao   = db.trackedHabitDao()
    private val checkInDao   = db.checkInDao()
    private val profileDao   = db.userProfileDao()
    private val pendingAiDao = db.pendingAiHabitDao()
    private val dailyDao     = db.dailyHabitDao()          // ← NEW
    private val claude       = ClaudeHabitAnalyzer(claudeApiKey)

    val trackedHabits: Flow<List<TrackedHabitEntity>>   = trackedDao.observeAll()
    val profile: Flow<UserProfileEntity?>               = profileDao.observeProfile()
    val recentCheckIns: Flow<List<CheckInEntity>>       = checkInDao.observeRecent()
    val pendingAi: Flow<List<PendingAiHabitEntity>>     = pendingAiDao.observePending()
    val dailyHistory: Flow<List<DailyHabitEntity>>      = dailyDao.observeHistory()  // ← NEW

    // ── Profile ──────────────────────────────────────────────────────────

    suspend fun ensureProfile() {
        if (profileDao.getProfile() == null) {
            val monday = weekStartEpochDay()
            profileDao.upsert(
                UserProfileEntity(
                    onboardingComplete    = false,
                    totalXp              = 0,
                    weeklyXp             = 0,
                    weekStartEpochDay    = monday,
                    habitsBeaten         = 0,
                    firstAiScanDone      = false,
                    leaderboardVisibility = "public",
                    moneySavedInr        = 0L,
                    streakFreezeTokens   = 2,
                    currentLevel         = 1,
                ),
            )
        }
    }

    // ── Onboarding ───────────────────────────────────────────────────────

    suspend fun completeOnboarding(selectedLibraryIds: List<String>) {
        val now = System.currentTimeMillis()
        for (libraryId in selectedLibraryIds.distinct()) {
            val lib = HabitLibrary.byId(libraryId) ?: continue
            trackedDao.insert(
                TrackedHabitEntity(
                    id                = UUID.randomUUID().toString(),
                    libraryHabitId    = lib.id,
                    customTitle       = null,
                    category          = lib.category.name,
                    severity          = lib.severity,
                    currentStreak     = 0,
                    longestStreak     = 0,
                    lastCleanEpochDay = null,
                    isBeaten          = false,
                    createdAtEpochMs  = now,
                    isSensitive       = lib.category == HabitCategory.Substance,
                ),
            )
        }
        val existing = profileDao.getProfile() ?: return
        profileDao.upsert(existing.copy(onboardingComplete = true))
    }

    // ── Habit tracking ───────────────────────────────────────────────────

    suspend fun addTrackedHabitFromLibrary(libraryId: String) {
        val lib = HabitLibrary.byId(libraryId) ?: return
        if (trackedDao.getByLibraryId(libraryId) != null) return
        val now = System.currentTimeMillis()
        trackedDao.insert(
            TrackedHabitEntity(
                id                = UUID.randomUUID().toString(),
                libraryHabitId    = lib.id,
                customTitle       = null,
                category          = lib.category.name,
                severity          = lib.severity,
                currentStreak     = 0,
                longestStreak     = 0,
                lastCleanEpochDay = null,
                isBeaten          = false,
                createdAtEpochMs  = now,
                isSensitive       = lib.category == HabitCategory.Substance,
            ),
        )
    }

    suspend fun addCustomHabit(title: String, category: HabitCategory, severity: Int) {
        val now = System.currentTimeMillis()
        trackedDao.insert(
            TrackedHabitEntity(
                id                = UUID.randomUUID().toString(),
                libraryHabitId    = null,
                customTitle       = title,
                category          = category.name,
                severity          = severity,
                currentStreak     = 0,
                longestStreak     = 0,
                lastCleanEpochDay = null,
                isBeaten          = false,
                createdAtEpochMs  = now,
                isSensitive       = false,
            ),
        )
    }

    // ── Quit challenge check-in ──────────────────────────────────────────

    suspend fun submitCheckIn(habitId: String, outcome: CheckInOutcome) {
        val habit = trackedDao.getById(habitId) ?: return
        if (habit.isBeaten) return

        val today = LocalDate.now().toEpochDay()
        rollWeeklyWindowIfNeeded()

        var profile    = profileDao.getProfile() ?: return
        val baseXp     = Gamification.xpForDailyCheckIn(outcome)
        var bonusXp    = 0
        var newStreak  = habit.currentStreak
        var lastClean  = habit.lastCleanEpochDay
        var longest    = habit.longestStreak
        var beaten     = habit.isBeaten
        var habitsBeaten = profile.habitsBeaten
        var money      = profile.moneySavedInr

        when (outcome) {
            CheckInOutcome.Clean -> {
                newStreak = when {
                    lastClean == null        -> 1
                    today == lastClean       -> habit.currentStreak
                    today == lastClean + 1   -> habit.currentStreak + 1
                    else                     -> 1
                }
                lastClean = today
                longest   = maxOf(longest, newStreak)
                money    += 25L
                if (newStreak == 7) bonusXp += Gamification.xpSevenDayMilestone()
                if (newStreak >= 30 && !habit.isBeaten) {
                    beaten        = true
                    bonusXp      += Gamification.xpHabitBeaten()
                    habitsBeaten += 1
                }
            }
            CheckInOutcome.Partial -> { /* streak unchanged */ }
            CheckInOutcome.Slip    -> { newStreak = 0 }
        }

        val totalGain = baseXp + bonusXp
        val newTotalXp = profile.totalXp + totalGain
        profile = profile.copy(
            totalXp      = newTotalXp,
            weeklyXp     = profile.weeklyXp + totalGain,
            habitsBeaten = habitsBeaten,
            moneySavedInr = money,
            currentLevel = Gamification.levelFromXp(newTotalXp),
        )
        profileDao.upsert(profile)

        trackedDao.update(
            habit.copy(
                currentStreak     = newStreak,
                longestStreak     = longest,
                lastCleanEpochDay = lastClean,
                isBeaten          = beaten,
            ),
        )

        val outcomeCode = when (outcome) {
            CheckInOutcome.Clean   -> "CLEAN"
            CheckInOutcome.Partial -> "PARTIAL"
            CheckInOutcome.Slip    -> "SLIP"
        }
        checkInDao.upsert(
            CheckInEntity(
                id               = UUID.randomUUID().toString(),
                habitId          = habitId,
                epochDay         = today,
                outcome          = outcomeCode,
                xpEarned         = totalGain,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    // ── NEW: Daily notification habit ────────────────────────────────────

    /** Insert the chosen habit for today into the daily_habits table. */
    suspend fun insertDailyHabit(epochDay: Long, libraryHabitId: String) {
        dailyDao.insert(
            DailyHabitEntity(
                epochDay            = epochDay,
                libraryHabitId      = libraryHabitId,
                notificationFiredMs = System.currentTimeMillis(),
            ),
        )
    }

    /** Get today's daily habit record (null if not yet assigned). */
    suspend fun getDailyHabitForDay(epochDay: Long): DailyHabitEntity? =
        dailyDao.getForDay(epochDay)

    /** Returns the library habit IDs shown in the last 60 days (for no-repeat rule). */
    suspend fun recentDailyHabitIds(): List<String> = dailyDao.recentHabitIds()

    /** Mark the evening reminder as sent for today. */
    suspend fun markEveningReminderSent(epochDay: Long) {
        dailyDao.setEveningReminderFired(epochDay, System.currentTimeMillis())
    }

    /**
     * Called by NotificationResponseReceiver when user taps an action button.
     *
     * AVOIDED   → award XP (10/20/30 by severity), update awareness streak XP
     * I_DO_THIS → add this habit to tracked quit challenges, start challenge
     */
    suspend fun recordDailyNotificationResponse(
        epochDay: Long,
        libraryId: String,
        response: String,
    ) {
        val existing = dailyDao.getForDay(epochDay) ?: return
        if (existing.response != null) return   // already responded today

        val lib     = HabitLibrary.byId(libraryId) ?: return
        var profile = profileDao.getProfile() ?: return
        rollWeeklyWindowIfNeeded()

        val xpEarned: Int
        when (response) {
            NotificationHelper.RESPONSE_AVOIDED -> {
                // Award XP based on severity (10/20/30)
                xpEarned = Gamification.xpForDailyNotificationAvoided(lib.severity)

                val newTotalXp = profile.totalXp + xpEarned

                // Award freeze token every 7-day awareness streak (simple increment check)
                val newTokens = if (Gamification.shouldEarnFreezeToken(profile.totalXp / 10))
                    (profile.streakFreezeTokens + 1).coerceAtMost(10)
                else profile.streakFreezeTokens

                profileDao.upsert(
                    profile.copy(
                        totalXp            = newTotalXp,
                        weeklyXp           = profile.weeklyXp + xpEarned,
                        streakFreezeTokens = newTokens,
                        currentLevel       = Gamification.levelFromXp(newTotalXp),
                    ),
                )
            }

            NotificationHelper.RESPONSE_I_DO_THIS -> {
                // Start a quit challenge for this habit
                addTrackedHabitFromLibrary(libraryId)
                xpEarned = 0   // XP comes from the quit challenge check-ins

                // Award first quit challenge XP if this is the first ever
                val tracked = trackedHabits.first()
                if (tracked.size == 1) {
                    val bonus = Gamification.xpFirstQuitChallengeStarted()
                    val newTotal = profile.totalXp + bonus
                    profileDao.upsert(
                        profile.copy(
                            totalXp      = newTotal,
                            weeklyXp     = profile.weeklyXp + bonus,
                            currentLevel = Gamification.levelFromXp(newTotal),
                        ),
                    )
                }
            }

            else -> xpEarned = 0
        }

        dailyDao.setResponse(epochDay, response, xpEarned)
    }

    // ── AI scan ──────────────────────────────────────────────────────────

    suspend fun runAiScan(routineText: String, isWeekly: Boolean): AiScanResult {
        rollWeeklyWindowIfNeeded()
        if (routineText.isBlank()) {
            return AiScanResult(
                usedCloudAi    = false,
                insightsApplied = 0,
                newSuggestions  = 0,
                note            = "Add a routine description first.",
            )
        }

        val cloud       = claude.analyze(routineText)
        val usedCloud   = cloud.isSuccess
        val detections  = if (cloud.isSuccess) {
            cloud.getOrNull().orEmpty()
        } else {
            LocalRoutineAnalyzer.analyze(routineText)
        }

        val filtered = detections.filter { it.confidence >= 0.6f }
        pendingAiDao.clearPending()
        val now = System.currentTimeMillis()

        val tracked       = trackedHabits.first()
        val trackedLibIds = tracked.mapNotNull { it.libraryHabitId }.toSet()

        val mergeMap = linkedMapOf<String, MutableList<String>>()
        for (d in filtered) {
            val lib = d.libraryMatchId ?: continue
            if (lib !in trackedLibIds) continue
            mergeMap.getOrPut(lib) { mutableListOf() }.add("${d.title}: ${d.personalisedHarm}")
        }
        var insightsApplied = 0
        for ((libId, parts) in mergeMap) {
            val entity = tracked.firstOrNull { it.libraryHabitId == libId } ?: continue
            val merged = parts.distinct().joinToString("\n\n").take(1200)
            trackedDao.update(entity.copy(lastAiInsight = merged, lastAiScanEpochMs = now))
            insightsApplied++
        }

        val pendingDetections = filtered.filter { d ->
            when {
                d.libraryMatchId == null              -> true
                d.libraryMatchId !in trackedLibIds    -> true
                else                                  -> false
            }
        }
        if (pendingDetections.isNotEmpty()) {
            pendingAiDao.insertAll(
                pendingDetections.map {
                    PendingAiHabitEntity(
                        id               = LocalRoutineAnalyzer.newPendingId(),
                        title            = it.title,
                        category         = it.category.name,
                        severity         = it.severity,
                        personalisedHarm = it.personalisedHarm,
                        quitBenefit      = it.quitBenefit,
                        confidence       = it.confidence,
                        libraryMatchId   = it.libraryMatchId,
                        createdAtEpochMs = now,
                        accepted         = null,
                    )
                },
            )
        }

        var profile = profileDao.getProfile() ?: return AiScanResult(
            usedCloudAi    = usedCloud,
            insightsApplied = insightsApplied,
            newSuggestions  = pendingDetections.size,
            note            = scanNote(usedCloud, filtered.isEmpty(), cloud.exceptionOrNull()),
        )

        var xpAdd = 0
        if (!profile.firstAiScanDone) {
            xpAdd  += Gamification.xpFirstAiScan()
            profile = profile.copy(firstAiScanDone = true)
        }
        if (isWeekly) xpAdd += Gamification.xpWeeklyRescan()

        if (xpAdd > 0) {
            val newTotal = profile.totalXp + xpAdd
            profileDao.upsert(
                profile.copy(
                    totalXp      = newTotal,
                    weeklyXp     = profile.weeklyXp + xpAdd,
                    currentLevel = Gamification.levelFromXp(newTotal),
                ),
            )
        }

        return AiScanResult(
            usedCloudAi    = usedCloud,
            insightsApplied = insightsApplied,
            newSuggestions  = pendingDetections.size,
            note            = scanNote(usedCloud, filtered.isEmpty(), cloud.exceptionOrNull()),
        )
    }

    private fun scanNote(usedCloud: Boolean, emptyResults: Boolean, err: Throwable?): String? {
        return when {
            usedCloud && emptyResults -> "Connected to Claude — no habits met the ≥0.6 confidence threshold."
            !usedCloud && err != null -> {
                val reason = err.message?.take(100)?.trim().orEmpty()
                if (reason.isEmpty()) "Using on-device keyword analysis (offline)."
                else "Using on-device analysis — $reason"
            }
            usedCloud -> null
            else -> "Using on-device keyword analysis."
        }
    }

    suspend fun acceptPendingAi(id: String) {
        val pending = pendingAiDao.getById(id) ?: return
        pendingAiDao.setAccepted(id, true)
        if (pending.libraryMatchId != null) {
            addTrackedHabitFromLibrary(pending.libraryMatchId)
        } else {
            addCustomHabit(
                title    = pending.title,
                category = runCatching { HabitCategory.valueOf(pending.category) }
                    .getOrDefault(HabitCategory.Mental),
                severity = pending.severity,
            )
        }
    }

    suspend fun dismissPendingAi(id: String) {
        pendingAiDao.setAccepted(id, false)
    }

    // ── Leaderboard ──────────────────────────────────────────────────────

    suspend fun buildLeaderboard(): Pair<List<LeaderboardRow>, LeaderboardRow> {
        val habits     = trackedHabits.first()
        val profile    = profile.first()
            ?: UserProfileEntity(
                onboardingComplete    = false,
                totalXp              = 0,
                weeklyXp             = 0,
                weekStartEpochDay    = weekStartEpochDay(),
                habitsBeaten         = 0,
                firstAiScanDone      = false,
                leaderboardVisibility = "public",
                moneySavedInr        = 0L,
            )
        val bestStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        val attempted  = habits.size
        val userScore  = Gamification.leaderboardScore(
            habitsBeaten    = profile.habitsBeaten,
            bestStreak      = bestStreak,
            totalXp         = profile.totalXp,
            habitsAttempted = attempted,
        )
        val userRow = LeaderboardRow(name = "You", score = userScore, isSelf = true)
        val mock = listOf(
            LeaderboardRow("Rahul · Delhi",       userScore + 4200, false),
            LeaderboardRow("Priya · Pune",         userScore + 2100, false),
            LeaderboardRow("Suresh · Mumbai",      userScore + 800,  false),
            LeaderboardRow("Ananya · Bangalore",   userScore - 400,  false),
            LeaderboardRow("Vikram · Chennai",     userScore - 1200, false),
        )
        val combined = (mock + userRow).sortedByDescending { it.score }
        return combined to userRow
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private suspend fun rollWeeklyWindowIfNeeded() {
        val profile = profileDao.getProfile() ?: return
        val currentMonday = weekStartEpochDay()
        if (profile.weekStartEpochDay != currentMonday) {
            profileDao.upsert(profile.copy(weeklyXp = 0, weekStartEpochDay = currentMonday))
        }
    }

    private fun weekStartEpochDay(): Long {
        val today  = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        return monday.toEpochDay()
    }
}

data class LeaderboardRow(
    val name: String,
    val score: Long,
    val isSelf: Boolean,
)
