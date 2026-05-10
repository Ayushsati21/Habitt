package com.example.habit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habit.data.AiScanResult
import com.example.habit.data.local.entity.PendingAiHabitEntity
import com.example.habit.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScanScreen(
    pending: List<PendingAiHabitEntity>,
    scanFeedback: AiScanResult?,
    onConsumeScanFeedback: () -> Unit,
    onBack: () -> Unit,
    onAnalyze: (String, Boolean) -> Unit,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
) {
    var routine by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(scanFeedback) {
        val result = scanFeedback ?: return@LaunchedEffect
        val bits = buildList {
            add(
                if (result.usedCloudAi) "Claude connected" else "On-device analysis",
            )
            if (result.insightsApplied > 0) add("${result.insightsApplied} tracked habit(s) updated with new AI notes")
            if (result.newSuggestions > 0) add("${result.newSuggestions} new suggestion(s) to review")
            result.note?.let { add(it) }
        }
        snackbarHostState.showSnackbar(bits.joinToString(" · "))
        onConsumeScanFeedback()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Discovery",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("AI habit scan", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(
                title = "Describe your day",
                subtitle = "Uses Claude when `CLAUDE_API_KEY` is set in local.properties; otherwise on-device keywords. " +
                    "Matched habits you already track get updated notes on your dashboard.",
            )
            OutlinedTextField(
                value = routine,
                onValueChange = { routine = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                label = { Text("Routine") },
                placeholder = { Text("Wake-up, meals, work, stress, screens, sleep…") },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
            FilledTonalButton(
                onClick = { onAnalyze(routine, false) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) { Text("Run scan") }
            OutlinedButton(
                onClick = { onAnalyze(routine, true) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) { Text("Weekly re-scan (+XP when eligible)") }
            Text(
                "Review detections",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (pending.isEmpty()) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Text(
                        "No pending habits. Try words like smoking, chips, YouTube, desk, coffee, gaming.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                pending.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${item.category} · severity ${item.severity}/3 · conf ${"%.2f".format(item.confidence)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(item.personalisedHarm, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                item.quitBenefit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { onAccept(item.id) },
                                    shape = MaterialTheme.shapes.medium,
                                ) { Text("Accept") }
                                OutlinedButton(
                                    onClick = { onDismiss(item.id) },
                                    shape = MaterialTheme.shapes.medium,
                                ) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }
        }
    }
}
