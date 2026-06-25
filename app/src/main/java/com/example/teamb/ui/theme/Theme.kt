package com.example.teamb.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GarminLightScheme = lightColorScheme(
    primary = GarminBlue,
    onPrimary = CardSurface,
    primaryContainer = AccentBlue,
    onPrimaryContainer = GarminBlue,
    secondary = GarminBlueDark,
    onSecondary = CardSurface,
    background = Canvas,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = AccentBlue,
    onSurfaceVariant = TextSecondary,
    outline = InputBorder,
    outlineVariant = CardBorder,
    error = IssueText,
)

@Composable
fun TeamBTheme(
    // The design is a single light Garmin theme; dark/dynamic are intentionally disabled.
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = Canvas.toArgb()
        window.navigationBarColor = CardSurface.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(
        colorScheme = GarminLightScheme,
        typography = Typography,
        content = content,
    )
}
