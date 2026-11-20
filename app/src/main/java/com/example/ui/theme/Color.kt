package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Mode — Airy Periwinkle & Lilac Pastels
val LightBackground = Color(0xFFF3F5FF)
val LightBackgroundBottom = Color(0xFFF7F2FF)
val LightSurface = Color(0xD9FFFFFF) // Frosted translucent white
val LightSurfaceVariant = Color(0xFFEAEFFE)
val LightPrimary = Color(0xFF4F6BF6) // Periwinkle Cobalt
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0E7FF)
val LightOnPrimaryContainer = Color(0xFF1E2B7A)
val LightSecondary = Color(0xFF8B5CF6) // Soft Lilac Violet
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFEDE9FE)
val LightOnSecondaryContainer = Color(0xFF3B1D8E)
val LightTertiary = Color(0xFF38BDF8)
val LightOnTertiary = Color(0xFF082F49)
val LightTertiaryContainer = Color(0xFFE0F2FE)
val LightOnTertiaryContainer = Color(0xFF0C4A6E)
val LightOnBackground = Color(0xFF17193B)
val LightOnSurface = Color(0xFF17193B)
val LightOnSurfaceVariant = Color(0xFF565B85)
val LightOutline = Color(0xFFC7D2FE)
val LightOutlineVariant = Color(0xFFDDE3FA)
val LightError = Color(0xFFE11D48)
val LightOnError = Color(0xFFFFFFFF)

// Dark Mode — Midnight Indigo & Glowing Electric Blue-Violet
val DarkBackground = Color(0xFF070B19)
val DarkBackgroundBottom = Color(0xFF0E1230)
val DarkSurface = Color(0xB8121938) // Frosted midnight glass
val DarkSurfaceVariant = Color(0xFF1A2248)
val DarkPrimary = Color(0xFF60A5FA) // Electric Sky Blue
val DarkOnPrimary = Color(0xFF061539)
val DarkPrimaryContainer = Color(0xFF1E2B6B)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)
val DarkSecondary = Color(0xFFA78BFA) // Luminous Lavender Violet
val DarkOnSecondary = Color(0xFF1E1047)
val DarkSecondaryContainer = Color(0xFF2E1F66)
val DarkOnSecondaryContainer = Color(0xFFEDE9FE)
val DarkTertiary = Color(0xFF38BDF8)
val DarkOnTertiary = Color(0xFF082F49)
val DarkTertiaryContainer = Color(0xFF12365E)
val DarkOnTertiaryContainer = Color(0xFFE0F2FE)
val DarkOnBackground = Color(0xFFF1F5FF)
val DarkOnSurface = Color(0xFFF1F5FF)
val DarkOnSurfaceVariant = Color(0xFFA2ADD1)
val DarkOutline = Color(0xFF3B4882)
val DarkOutlineVariant = Color(0xFF242E5C)
val DarkError = Color(0xFFFB7185)
val DarkOnError = Color(0xFF4C0519)

@Immutable
data class AtmospherePalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val orbBlue: Color,
    val orbPurple: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val heroGradStart: Color,
    val heroGradMid: Color,
    val heroGradEnd: Color,
    val heroOnPrimary: Color,
    val heroSubtleBg: Color,
    val accentBlue: Color,
    val accentPurple: Color,
    val navBarFill: Color,
)

val LightAtmospherePalette = AtmospherePalette(
    isDark = false,
    bgTop = Color(0xFFF1F4FF),
    bgBottom = Color(0xFFF7F1FF),
    orbBlue = Color(0xFFA5B4FC).copy(alpha = 0.34f),
    orbPurple = Color(0xFFD8B4FE).copy(alpha = 0.32f),
    glassFillTop = Color(0xE8FFFFFF),
    glassFillBottom = Color(0xCCF5F3FF),
    glassBorderStart = Color(0xFFBAC8FF),
    glassBorderEnd = Color(0xFFDDD6FE),
    heroGradStart = Color(0xFF4F6BF6),
    heroGradMid = Color(0xFF6B5CF6),
    heroGradEnd = Color(0xFF9353F6),
    heroOnPrimary = Color(0xFFFFFFFF),
    heroSubtleBg = Color(0x33FFFFFF),
    accentBlue = Color(0xFF4F6BF6),
    accentPurple = Color(0xFF8B5CF6),
    navBarFill = Color(0xEBFFFFFF),
)

val DarkAtmospherePalette = AtmospherePalette(
    isDark = true,
    bgTop = Color(0xFF060A17),
    bgBottom = Color(0xFF0F1336),
    orbBlue = Color(0xFF2563EB).copy(alpha = 0.24f),
    orbPurple = Color(0xFF7C3AED).copy(alpha = 0.22f),
    glassFillTop = Color(0xBF141C3E),
    glassFillBottom = Color(0xA6101532),
    glassBorderStart = Color(0x7060A5FA),
    glassBorderEnd = Color(0x59A78BFA),
    heroGradStart = Color(0xFF1D4ED8),
    heroGradMid = Color(0xFF4338CA),
    heroGradEnd = Color(0xFF6D28D9),
    heroOnPrimary = Color(0xFFF8FAFF),
    heroSubtleBg = Color(0x2BFFFFFF),
    accentBlue = Color(0xFF60A5FA),
    accentPurple = Color(0xFFA78BFA),
    navBarFill = Color(0xEB0E1430),
)

@Immutable
data class SemanticStatusPalette(
    val greenBg: Color,
    val greenFg: Color,
    val greenAccent: Color,
    val yellowBg: Color,
    val yellowFg: Color,
    val yellowAccent: Color,
    val redBg: Color,
    val redFg: Color,
    val redAccent: Color,
    val grayBg: Color,
    val grayFg: Color,
    val grayAccent: Color,
    val darkBg: Color,
    val darkFg: Color,
    val darkAccent: Color,
    val blueBg: Color,
    val blueFg: Color,
    val blueAccent: Color,
)

val LightStatusPalette = SemanticStatusPalette(
    greenBg = Color(0xFFDCFCE7),
    greenFg = Color(0xFF14532D),
    greenAccent = Color(0xFF16A34A),
    yellowBg = Color(0xFFFEF3C7),
    yellowFg = Color(0xFF78350F),
    yellowAccent = Color(0xFFD97706),
    redBg = Color(0xFFFFE4E6),
    redFg = Color(0xFF881337),
    redAccent = Color(0xFFE11D48),
    grayBg = Color(0xFFEEF0FA),
    grayFg = Color(0xFF3E436B),
    grayAccent = Color(0xFF646C9A),
    darkBg = Color(0xFF17193B),
    darkFg = Color(0xFFF8FAFF),
    darkAccent = Color(0xFF0F1029),
    blueBg = Color(0xFFE0E7FF),
    blueFg = Color(0xFF1E3A8A),
    blueAccent = Color(0xFF4F6BF6),
)

val DarkStatusPalette = SemanticStatusPalette(
    greenBg = Color(0xFF14532D).copy(alpha = 0.65f),
    greenFg = Color(0xFF86EFAC),
    greenAccent = Color(0xFF4ADE80),
    yellowBg = Color(0xFF78350F).copy(alpha = 0.65f),
    yellowFg = Color(0xFFFDE68A),
    yellowAccent = Color(0xFFFBBF24),
    redBg = Color(0xFF881337).copy(alpha = 0.65f),
    redFg = Color(0xFFFDA4AF),
    redAccent = Color(0xFFFB7185),
    grayBg = Color(0xFF232C57).copy(alpha = 0.85f),
    grayFg = Color(0xFFCBD5F5),
    grayAccent = Color(0xFF94A3D1),
    darkBg = Color(0xFF050814),
    darkFg = Color(0xFFE2E8FF),
    darkAccent = Color(0xFF6366F1),
    blueBg = Color(0xFF1E3A8A).copy(alpha = 0.68f),
    blueFg = Color(0xFF93C5FD),
    blueAccent = Color(0xFF60A5FA),
)

val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }
val LocalAtmospherePalette = staticCompositionLocalOf { LightAtmospherePalette }
