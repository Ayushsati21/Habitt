package com.example.habit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habit.data.LeaderboardRow
import com.example.habit.ui.components.SectionHeader

@Composable
fun LeaderboardScreen(
    rows: List<LeaderboardRow>?,
    userRow: LeaderboardRow?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            title = "Leaderboard",
            subtitle = "Habits beaten weigh heaviest — then streak, XP, and habits attempted.",
        )
        if (rows == null || userRow == null) {
            Text("Loading ranks…", style = MaterialTheme.typography.bodyMedium)
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(rows, key = { index, row -> "$index-${row.name}" }) { index, row ->
                val rank = index + 1
                val highlight = row.isSelf
                val medal = when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> null
                }
                Card(
                    colors = when {
                        highlight -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        rank <= 3 -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        else -> CardDefaults.cardColors()
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = if (rank <= 3) 2.dp else 0.dp),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (medal != null) {
                            Text(medal, style = MaterialTheme.typography.headlineSmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "#$rank · ${row.name}",
                                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "Score ${row.score}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (highlight) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    "You",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
