package com.lordyhas.sleeplab.ui.theme

import androidx.compose.ui.graphics.Color

// Night sky backdrop
val NightSkyTop = Color(0xFF0E1430)
val NightSkyBottom = Color(0xFF05070F)

// Primary Sleep Palette
val DeepNightBackground = Color(0xFF070A14)
val MidnightSurface = Color(0xFF111729)
val MidnightSurfaceVariant = Color(0xFF1C2438)
val MidnightSurfaceCard = Color(0xB3141B30) // translucent so the sky shows through
val CardBorder = Color(0x14FFFFFF)

val SleepIndigoPrimary = Color(0xFF6366F1)
val SleepIndigoLight = Color(0xFFA5B4FC)
val SleepTealAccent = Color(0xFF2DD4BF)
val SleepTealDark = Color(0xFF0F766E)
val MoonGlow = Color(0xFFFDE68A)

val SnoreLow = Color(0xFF34D399)     // Green (< 45 dB)
val SnoreMedium = Color(0xFFFBBF24)  // Amber (45-60 dB)
val SnoreHigh = Color(0xFFF87171)    // Red (> 60 dB)

/** Severity color for a sound level / intensity score, matching the detection bands. */
fun snoreLevelColor(db: Float): Color = when {
    db < 45f -> SnoreLow
    db < 60f -> SnoreMedium
    else -> SnoreHigh
}

// Material 3 Dark Palette
val DarkPrimary = Color(0xFFA5B4FC)
val DarkOnPrimary = Color(0xFF1E1B4B)
val DarkPrimaryContainer = Color(0xFF312E81)
val DarkOnPrimaryContainer = Color(0xFFE0E7FF)

val DarkSecondary = Color(0xFF2DD4BF)
val DarkOnSecondary = Color(0xFF042F2E)
val DarkSecondaryContainer = Color(0xFF134E4A)
val DarkOnSecondaryContainer = Color(0xFFCCFBF1)

val DarkTertiary = Color(0xFFFDE68A)
val DarkOnTertiary = Color(0xFF422006)

val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF450A0A)
val DarkErrorContainer = Color(0xFF7F1D1D)
val DarkOnErrorContainer = Color(0xFFFEE2E2)

val DarkBackground = DeepNightBackground
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkSurface = MidnightSurface
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = MidnightSurfaceVariant
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF475569)
val DarkOutlineVariant = Color(0xFF1E293B)
val DarkSurfaceContainerLowest = Color(0xFF060912)
val DarkSurfaceContainerLow = Color(0xFF0C1120)
val DarkSurfaceContainer = Color(0xFF0F1526)
val DarkSurfaceContainerHigh = Color(0xFF171F35)
val DarkSurfaceContainerHighest = Color(0xFF1F2842)

// Material 3 Light Palette
val LightPrimary = Color(0xFF4F46E5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0E7FF)
val LightOnPrimaryContainer = Color(0xFF312E81)

val LightSecondary = Color(0xFF0D9488)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCCFBF1)
val LightOnSecondaryContainer = Color(0xFF115E59)

val LightTertiary = Color(0xFFB45309)
val LightOnTertiary = Color(0xFFFFFFFF)

val LightBackground = Color(0xFFF8FAFC)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF475569)
