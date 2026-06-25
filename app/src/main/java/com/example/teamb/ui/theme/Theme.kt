package com.example.teamb.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun schemeFor(c: AppColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.accent,
        onPrimary = OnBrand,
        primaryContainer = c.accentBlue,
        onPrimaryContainer = c.accent,
        secondary = BrandCyan,
        onSecondary = OnBrand,
        background = c.canvas,
        onBackground = c.textPrimary,
        surface = c.cardSurface,
        onSurface = c.textPrimary,
        surfaceVariant = c.accentBlue,
        onSurfaceVariant = c.textSecondary,
        outline = c.inputBorder,
        outlineVariant = c.cardBorder,
        error = c.issueText,
    )
} else {
    lightColorScheme(
        primary = Navy,
        onPrimary = OnBrand,
        primaryContainer = c.accentBlue,
        onPrimaryContainer = Navy,
        secondary = GarminBlueDark,
        onSecondary = OnBrand,
        background = c.canvas,
        onBackground = c.textPrimary,
        surface = c.cardSurface,
        onSurface = c.textPrimary,
        surfaceVariant = c.accentBlue,
        onSurfaceVariant = c.textSecondary,
        outline = c.inputBorder,
        outlineVariant = c.cardBorder,
        error = c.issueText,
    )
}

@Composable
fun TeamBTheme(
    darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = appColors.canvas.toArgb()
        window.navigationBarColor = appColors.cardSurface.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = schemeFor(appColors),
            typography = Typography,
            content = content,
        )
    }
}
