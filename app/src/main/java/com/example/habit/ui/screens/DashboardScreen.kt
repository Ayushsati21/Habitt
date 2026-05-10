package com.example.habit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habit.data.HabitLibrary
import com.example.habit.data.local.entity.TrackedHabitEntity
import com.example.habit.data.local.entity.UserProfileEntity
import com.example.habit.data.model.CheckInOutcome
import com.example.habit.ui.components.CompactStatTile
import com.example.habit.ui.components.HeroStatCard
import com.example.habit.ui.components.SectionHeader
import com.example.habit.ui.util.displaySubtitle
import com.example.habit.ui.util.displayTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: UserProfileEntity?,
    habits: List<TrackedHabitEntity>,
    todayHarmStat: String?,
    onCheckIn: (String, CheckInOutcome) -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenAi: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    var checkInTarget by remember { mutableStateOf<TrackedHabitEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = onOpenAi) {
                        Icon(Icons.Outlined.Psychology, contentDescription = "AI habit scan")
                    }
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Notify → Educate → Challenge → Reward → Compete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CompactStatTile(
                        label = "Tracked",
                        value = habits.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    CompactStatTile(
                        label = "Best streak",
                        value = habits.maxOfOrNull { it.currentStreak }?.toString() ?: "0",
                        modifier = Modifier.weight(1f),
                    )
                    CompactStatTile(
                        label = "XP · week",
                        value = profile?.weeklyXp?.toString() ?: "0",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                HeroStatCard(
                    title = "Harm stat of the day",
                    body = todayHarmStat ?: "Add habits and check in to surface a personalised, data-style nudge here.",
                )
            }
            item {
                SectionHeader(
                    title = "Your habits",
                    subtitle = "Tap a card for details. Check in once per day per habit.",
                )
            }
            if (habits.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        tonalElevation = 0.dp,
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Outlined.TrackChanges,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "No habits yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Use the Library tab or AI scan to add what you want to quit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(habits, key = { it.id }) { habit ->
                HabitProgressCard(
                    habit = habit,
                    onCheckInClick = { checkInTarget = habit },
                    onOpen = { onOpenHabit(habit.id) },
                )
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    checkInTarget?.let { habit ->
        CheckInDialog(
            title = habit.displayTitle(),
            onDismiss = { checkInTarget = null },
            onSelect = { outcome ->
                onCheckIn(habit.id, outcome)
                checkInTarget = null
            },
        )
    }
}

@Composable
private fun HabitProgressCard(
    habit: TrackedHabitEntity,
    onCheckInClick: () -> Unit,
    onOpen: () -> Unit,
) {
    val title = habit.displayTitle()
    val subtitle = habit.displaySubtitle()
    val target = 30
    val progress = (habit.currentStreak.coerceAtMost(target) / target.toFloat()).coerceIn(0f, 1f)
    val severityLabel = when (habit.severity) {
        3 -> "High"
        2 -> "Medium"
        else -> "Low"
    }
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        severityLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            if (habit.isSensitive) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                ) {
                    Text(
                        "If this feels out of control, consider professional support — tracking stays private.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            if (!habit.lastAiInsight.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Latest AI scan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            habit.lastAiInsight.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Streak ${habit.currentStreak} · Best ${habit.longestStreak}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Next milestone: day ${nextMilestone(habit.currentStreak)} · $target-day beat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onCheckInClick,
                    enabled = !habit.isBeaten,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(if (habit.isBeaten) "Beaten" else "Check in")
                }
            }
        }
    }
}

private fun nextMilestone(current: Int): Int {
    val milestones = listOf(7, 14, 21, 30)
    return milestones.firstOrNull { it > current } ?: 30
}

@Composable
private fun CheckInDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (CheckInOutcome) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Check in · $title", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Log today honestly. Partial success still earns XP.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Clean +10 XP · Partial +5 XP · Slip resets streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { onSelect(CheckInOutcome.Clean) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Avoided (clean)") }
                OutlinedButton(
                    onClick = { onSelect(CheckInOutcome.Partial) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Reduced (partial)") }
                OutlinedButton(
                    onClick = { onSelect(CheckInOutcome.Slip) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Slip") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

fun pickTodayHarmStat(habits: List<TrackedHabitEntity>): String? {
    val top = habits.maxByOrNull { it.severity } ?: return null
    val lib = top.libraryHabitId?.let { HabitLibrary.byId(it) }
    return lib?.keyStat
}
