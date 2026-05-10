package com.example.habit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Violet,
    onPrimary = White,
    primaryContainer = Violet.copy(alpha = 0.15f),
    onPrimaryContainer = Ink,
    secondary = Sky,
    onSecondary = White,
    secondaryContainer = Sky.copy(alpha = 0.2f),
    onSecondaryContainer = Ink2,
    tertiary = Mint,
    onTertiary = White,
    tertiaryContainer = Mint.copy(alpha = 0.2f),
    onTertiaryContainer = MintDark,
    background = Cream,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = White.copy(alpha = 0.6f),
    onSurfaceVariant = Ink2,
    outline = Muted,
    outlineVariant = Muted.copy(alpha = 0.4f),
    error = Peach,
    onError = White,
    errorContainer = PeachLight,
    onErrorContainer = Peach,
)

private val DarkColorScheme = darkColorScheme(
    primary = Violet.copy(alpha = 0.8f),
    onPrimary = White,
    primaryContainer = Violet.copy(alpha = 0.3f),
    onPrimaryContainer = White,
    secondary = Sky.copy(alpha = 0.8f),
    onSecondary = White,
    secondaryContainer = Sky.copy(alpha = 0.3f),
    onSecondaryContainer = White,
    tertiary = Mint.copy(alpha = 0.8f),
    onTertiary = White,
    tertiaryContainer = Mint.copy(alpha = 0.3f),
    onTertiaryContainer = White,
    background = Ink,
    onBackground = Cream,
    surface = Ink2,
    onSurface = Cream,
    surfaceVariant = Ink2.copy(alpha = 0.7f),
    onSurfaceVariant = Muted,
    outline = Muted,
    outlineVariant = Muted.copy(alpha = 0.5f),
    error = Peach,
    onError = White,
)

@Composable
fun HabitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Set false to use the Habit palette; dynamic color overrides brand on Android 12+. */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HabitShapes,
        content = content,
    )
}
