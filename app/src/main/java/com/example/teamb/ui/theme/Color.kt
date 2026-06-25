package com.example.teamb.ui.theme

import androidx.compose.ui.graphics.Color

// Garmin design system palette (from the provided mockups / SVGs).
//
// Brand identity. #6DCFF6 ("Garmin sky") is the primary brand color — it carries the brand
// across logos, hero gradients, selected states and accents. Solid action buttons and text use
// the deeper [Navy] for legibility (white text on sky would fail contrast), matching the way the
// reference mockups render buttons. Together they form the navy → cyan → sky gradient.
val BrandSky = Color(0xFF6DCFF6)        // primary brand color (identity, gradients, accents)
val BrandCyan = Color(0xFF19A9E5)       // cyan mid-accent in gradients / iconography
val Navy = Color(0xFF1E3A8A)            // deep brand blue — actions, emphasis, on-light text
val GarminBlueMid = Color(0xFF005BAC)   // classic Garmin blue — gradient mid stop
val BrandWash = Color(0xFFCFE4FF)       // light wash (gradient end on soft accent surfaces)

// [GarminBlue] is the workhorse "action/text" color used across the app (button fills, selected
// states, focus borders, links). Repointed to [Navy] so those surfaces stay legible and match the
// reference buttons, while [BrandSky] leads the brand identity.
val GarminBlue = Navy
val GarminBlueDark = Color(0xFF152C66)
val GarminBlueLight = BrandCyan
val GarminGradientEnd = BrandSky

val Canvas = Color(0xFFF6F8FC)
val AccentBlue = Color(0xFFEAF3FF)
val CardSurface = Color(0xFFFFFFFF)
val CardBorder = Color(0xFFE5ECF5)
val InputFill = Color(0xFFF9FBFE)
val InputBorder = Color(0xFFD9E1EC)

val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF64748B)
val TextDisabled = Color(0xFF94A3B8)
val DisabledFill = Color(0xFFE2E8F2)

val PositiveBg = Color(0xFFDCFCE7)
val PositiveText = Color(0xFF15803D)
val IssueBg = Color(0xFFFEE2E2)
val IssueText = Color(0xFFB91C1C)
val WarningBg = Color(0xFFFEF3C7)
val WarningText = Color(0xFFA16207)

val FlameOrange = Color(0xFFF59E0B)

// Legacy names kept so the generated theme scaffolding still resolves.
val Purple80 = GarminBlueLight
val PurpleGrey80 = TextSecondary
val Pink80 = AccentBlue
val Purple40 = GarminBlue
val PurpleGrey40 = TextSecondary
val Pink40 = GarminBlueDark
