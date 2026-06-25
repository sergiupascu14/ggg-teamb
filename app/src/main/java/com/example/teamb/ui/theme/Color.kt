package com.example.teamb.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Brand identity — constant across light & dark.
//
// #6DCFF6 ("Garmin sky") is the primary brand color. Solid action surfaces use the deeper [Navy]
// with white ([OnBrand]) text. Together they form the navy → cyan → sky gradient.
// ---------------------------------------------------------------------------
val BrandSky = Color(0xFF6DCFF6)        // primary brand color (identity, gradients, accents)
val BrandCyan = Color(0xFF19A9E5)       // cyan mid-accent in gradients / iconography
val Navy = Color(0xFF1E3A8A)            // deep brand blue — solid fills carrying white text
val GarminBlueMid = Color(0xFF005BAC)   // classic Garmin blue — gradient mid stop
val BrandWash = Color(0xFFCFE4FF)       // light wash (gradient end on soft accent surfaces)
val OnBrand = Color(0xFFFFFFFF)         // white text/icons on navy/brand fills
val GarminBlueDark = Color(0xFF152C66)
val GarminBlueLight = BrandCyan
val GarminGradientEnd = BrandSky
val FlameOrange = Color(0xFFF59E0B)

// ---------------------------------------------------------------------------
// Theme-aware semantic palette.
//
// These swap between [LightAppColors] and [DarkAppColors] via [LocalAppColors]. Screens keep
// referencing the familiar token names (e.g. `Canvas`, `CardSurface`) — each is a @Composable
// accessor that resolves to the active scheme, so dark mode needs no per-screen edits. The one
// dual-role token, [GarminBlue], is the foreground accent (Navy in light, BrandSky in dark);
// solid fills use the constant [Navy]/[OnBrand] above.
// ---------------------------------------------------------------------------
data class AppColors(
    val canvas: Color,
    val accentBlue: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val inputFill: Color,
    val inputBorder: Color,
    val accent: Color, // foreground accent (links, selected text/icons, focus borders)
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val disabledFill: Color,
    val positiveBg: Color,
    val positiveText: Color,
    val issueBg: Color,
    val issueText: Color,
    val warningBg: Color,
    val warningText: Color,
    val isDark: Boolean,
)

val LightAppColors = AppColors(
    canvas = Color(0xFFF6F8FC),
    accentBlue = Color(0xFFEAF3FF),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE5ECF5),
    inputFill = Color(0xFFF9FBFE),
    inputBorder = Color(0xFFD9E1EC),
    accent = Navy,
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    textDisabled = Color(0xFF94A3B8),
    disabledFill = Color(0xFFE2E8F2),
    positiveBg = Color(0xFFDCFCE7),
    positiveText = Color(0xFF15803D),
    issueBg = Color(0xFFFEE2E2),
    issueText = Color(0xFFB91C1C),
    warningBg = Color(0xFFFEF3C7),
    warningText = Color(0xFFA16207),
    isDark = false,
)

val DarkAppColors = AppColors(
    canvas = Color(0xFF0B1220),
    accentBlue = Color(0xFF16263F),
    cardSurface = Color(0xFF131D31),
    cardBorder = Color(0xFF283449),
    inputFill = Color(0xFF18233A),
    inputBorder = Color(0xFF2E3C57),
    accent = BrandSky,
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFC2CCDC),
    textMuted = Color(0xFF94A3B8),
    textDisabled = Color(0xFF5A6B85),
    disabledFill = Color(0xFF243049),
    positiveBg = Color(0xFF14331F),
    positiveText = Color(0xFF4ADE80),
    issueBg = Color(0xFF3A1717),
    issueText = Color(0xFFF87171),
    warningBg = Color(0xFF3A2E12),
    warningText = Color(0xFFFBBF24),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// Theme-aware token accessors (resolve against the active [AppColors]).
val Canvas: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.canvas
val AccentBlue: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accentBlue
val CardSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.cardSurface
val CardBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.cardBorder
val InputFill: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.inputFill
val InputBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.inputBorder
val GarminBlue: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accent
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textMuted
val TextDisabled: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textDisabled
val DisabledFill: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.disabledFill
val PositiveBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.positiveBg
val PositiveText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.positiveText
val IssueBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.issueBg
val IssueText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.issueText
val WarningBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warningBg
val WarningText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warningText
