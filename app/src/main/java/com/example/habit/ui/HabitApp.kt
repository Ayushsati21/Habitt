package com.example.habit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.habit.data.LeaderboardRow
import com.example.habit.data.local.entity.PendingAiHabitEntity
import com.example.habit.data.local.entity.TrackedHabitEntity
import com.example.habit.data.local.entity.UserProfileEntity
import com.example.habit.ui.screens.AiScanScreen
import com.example.habit.ui.screens.DashboardScreen
import com.example.habit.ui.screens.HabitDetailScreen
import com.example.habit.ui.screens.LeaderboardScreen
import com.example.habit.ui.screens.LibraryHabitDetailScreen
import com.example.habit.ui.screens.LibraryScreen
import com.example.habit.ui.screens.NotificationsScreen
import com.example.habit.ui.screens.OnboardingScreen
import com.example.habit.ui.screens.ProfileScreen
import com.example.habit.ui.screens.pickTodayHarmStat
import com.example.habit.ui.viewmodel.HabitViewModel

@Composable
fun HabitApp(viewModel: HabitViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val habits by viewModel.trackedHabits.collectAsStateWithLifecycle()
    val pendingAi by viewModel.pendingAi.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.ensureProfile()
    }

    val currentProfile = profile
    when {
        currentProfile == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        !currentProfile.onboardingComplete -> {
            OnboardingScreen(onFinished = { viewModel.completeOnboarding(it) })
        }
        else -> {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "shell") {
                composable("shell") {
                    MainShell(
                        profile = currentProfile,
                        habits = habits,
                        pendingAi = pendingAi,
                        leaderboard = leaderboard,
                        onNavigate = { route -> navController.navigate(route) },
                        viewModel = viewModel,
                    )
                }
                composable("ai") {
                    val scanFeedback by viewModel.scanFeedback.collectAsStateWithLifecycle()
                    AiScanScreen(
                        pending = pendingAi,
                        scanFeedback = scanFeedback,
                        onConsumeScanFeedback = { viewModel.consumeScanFeedback() },
                        onBack = { navController.popBackStack() },
                        onAnalyze = { text, weekly -> viewModel.runAiScan(text, weekly) },
                        onAccept = { viewModel.acceptAiDetection(it) },
                        onDismiss = { viewModel.dismissAiDetection(it) },
                    )
                }
                composable("notifications") {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }
                composable("library/{libraryId}") { entry ->
                    val libraryId = entry.arguments?.getString("libraryId").orEmpty()
                    val tracked = habits.mapNotNull { it.libraryHabitId }.toSet().contains(libraryId)
                    LibraryHabitDetailScreen(
                        libraryId = libraryId,
                        alreadyTracked = tracked,
                        onBack = { navController.popBackStack() },
                        onTrack = {
                            viewModel.trackLibraryHabit(libraryId)
                            navController.popBackStack()
                        },
                    )
                }
                composable("habit/{habitId}") { entry ->
                    val id = entry.arguments?.getString("habitId").orEmpty()
                    val habit = habits.find { it.id == id }
                    HabitDetailScreen(habit = habit, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun MainShell(
    profile: UserProfileEntity,
    habits: List<TrackedHabitEntity>,
    pendingAi: List<PendingAiHabitEntity>,
    leaderboard: Pair<List<LeaderboardRow>, LeaderboardRow>?,
    onNavigate: (String) -> Unit,
    viewModel: HabitViewModel,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation,
            ) {
                val colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.MenuBook, contentDescription = null) },
                    label = { Text("Library") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                    label = { Text("Board") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    colors = colors,
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> DashboardScreen(
                    profile = profile,
                    habits = habits,
                    todayHarmStat = pickTodayHarmStat(habits),
                    onCheckIn = { id, outcome -> viewModel.submitCheckIn(id, outcome) },
                    onOpenHabit = { id -> onNavigate("habit/$id") },
                    onOpenAi = { onNavigate("ai") },
                    onOpenNotifications = { onNavigate("notifications") },
                )
                1 -> LibraryScreen(
                    trackedLibraryIds = habits.mapNotNull { it.libraryHabitId }.toSet(),
                    onOpenHabit = { libraryId -> onNavigate("library/$libraryId") },
                    onTrack = { libraryId -> viewModel.trackLibraryHabit(libraryId) },
                )
                2 -> LeaderboardScreen(rows = leaderboard?.first, userRow = leaderboard?.second)
                3 -> ProfileScreen(profile = profile, habits = habits)
            }
        }
    }
}
