package com.example.habit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habit.data.local.entity.TrackedHabitEntity
import com.example.habit.data.local.entity.UserProfileEntity
import com.example.habit.domain.Gamification
import com.example.habit.ui.components.SectionHeader

@Composable
fun ProfileScreen(
    profile: UserProfileEntity?,
    habits: List<TrackedHabitEntity>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            title    = "Profile",
            subtitle = "Your level, rank, streak records, and unlocked badges.",
        )
        if (profile == null) {
            Text("Loading…")
            return
        }

        val tier    = Gamification.rankTierLabel(profile.habitsBeaten)
        val level   = Gamification.levelFromXp(profile.totalXp)
        val levelLbl = Gamification.levelLabel(level)
        val longest = habits.maxOfOrNull { it.longestStreak } ?: 0

        // ── Hero gradient card ─────────────────────────────────────────
        Card(
            shape     = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            val brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                ),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(brush)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top,
                ) {
                    Column {
                        Text(
                            "Level $level · $levelLbl",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                        Text(
                            tier,
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                    ) {
                        Text(
                            "🧊 ${profile.streakFreezeTokens} freeze${if (profile.streakFreezeTokens == 1) "" else "s"}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Text(
                    "${profile.habitsBeaten} habits beaten · $longest-day best streak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                )

                // XP progress bar toward next level
                val nextLevelXp = Gamification.nextLevelXp(profile.totalXp)
                if (nextLevelXp != null) {
                    val prevLevelXp = when (level) {
                        1 -> 0; 2 -> 200; 3 -> 500; 4 -> 1000; 5 -> 2500; else -> 0
                    }
                    val progress = ((profile.totalXp - prevLevelXp).toFloat() /
                            (nextLevelXp - prevLevelXp)).coerceIn(0f, 1f)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${profile.totalXp} / $nextLevelXp XP to Level ${level + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                    LinearProgressIndicator(
                        progress    = { progress },
                        modifier    = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        trackColor  = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                    )
                } else {
                    Text(
                        "Max level reached · ${profile.totalXp} XP total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                }
            }
        }

        // ── Stats card ────────────────────────────────────────────────
        Card(
            shape     = MaterialTheme.shapes.large,
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatLine("Total XP",              profile.totalXp.toString())
                StatLine("Weekly XP",             profile.weeklyXp.toString())
                StatLine("Streak freeze tokens",  profile.streakFreezeTokens.toString())
                StatLine("Estimated savings",     "₹${profile.moneySavedInr}")
                StatLine("Rank tier",             tier)
            }
        }

        // ── Badges ────────────────────────────────────────────────────
        Text("Badges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        val badges = buildList {
            if (habits.any { it.currentStreak >= 1 } || profile.totalXp > 0)
                add("🌱 First Step — started logging")
            if (habits.any { it.longestStreak >= 7 })
                add("🔥 On Fire — 7-day streak reached")
            if (habits.any { it.longestStreak >= 14 })
                add("💪 Two-Week Titan")
            if (habits.any { it.longestStreak >= 30 })
                add("🥊 Habit Crusher — first habit beaten")
            if (profile.habitsBeaten >= 1)
                add("🏅 Habit Crusher — 30-day challenge complete")
            if (profile.moneySavedInr >= 5000)
                add("💰 Money Saver — ₹5,000 saved")
            if (profile.firstAiScanDone)
                add("🤖 Scanner — AI habit discovery used")
            if (profile.habitsBeaten >= 3)
                add("🎯 Multi-Quitter — 3 habits beaten")
            if (profile.habitsBeaten >= 7)
                add("🔴 Liberated — 7+ habits beaten")
        }

        if (badges.isEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ) {
                Text(
                    "Complete check-ins, daily notifications, and AI scans to unlock badges.",
                    modifier = Modifier.padding(14.dp),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            badges.forEach { b ->
                Surface(
                    shape    = MaterialTheme.shapes.medium,
                    color    = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        b,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        Text(
            "Privacy: leaderboard visibility is ${profile.leaderboardVisibility}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
