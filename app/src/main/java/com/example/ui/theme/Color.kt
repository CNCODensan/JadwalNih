package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.domain.MainTab

// Light Mode — Clean Neutral Canvas + Happy Pesisir Multi-Accents
val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDF1F7)
val LightPrimary = Color(0xFF1677FF)          // Pesisir Cobalt Blue
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0ECFF)
val LightOnPrimaryContainer = Color(0xFF0B3B8C)
val LightSecondary = Color(0xFF10B981)        // Command Center Fresh Mint
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD1FAE5)
val LightOnSecondaryContainer = Color(0xFF065F46)
val LightTertiary = Color(0xFFF97316)         // Pesisir Tangerine Orange
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFEDD5)
val LightOnTertiaryContainer = Color(0xFF9A3412)
val LightOnBackground = Color(0xFF111827)
val LightOnSurface = Color(0xFF111827)
val LightOnSurfaceVariant = Color(0xFF4B5563)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)
val LightError = Color(0xFFEE3E38)            // Pesisir Coral Red
val LightOnError = Color(0xFFFFFFFF)

// Dark Mode — Clean Midnight Slate (OPPO Dark Style) + Crisp White Text
val DarkBackground = Color(0xFF0E121B)
val DarkSurface = Color(0xFF1B212E)
val DarkSurfaceVariant = Color(0xFF262E3F)
val DarkPrimary = Color(0xFF3B82F6)           // Electric Cobalt Blue
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF1D355E)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)
val DarkSecondary = Color(0xFF34D399)         // Fresh Mint
val DarkOnSecondary = Color(0xFF062E22)
val DarkSecondaryContainer = Color(0xFF123F33)
val DarkOnSecondaryContainer = Color(0xFFD1FAE5)
val DarkTertiary = Color(0xFFFB923C)          // Tangerine Orange
val DarkOnTertiary = Color(0xFF2B1203)
val DarkTertiaryContainer = Color(0xFF4E2610)
val DarkOnTertiaryContainer = Color(0xFFFFEDD5)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFFCBD5E1)
val DarkOutline = Color(0xFF475569)
val DarkOutlineVariant = Color(0xFF2D374B)
val DarkError = Color(0xFFFB5A54)             // Bright Coral Red
val DarkOnError = Color(0xFFFFFFFF)

@Immutable
data class AtmospherePalette(
    val isDark: Boolean,
    val bgTop: Color,
    val cardBg: Color,
    val cardAltBg: Color,
    val cardBorder: Color,
    val heroSolidBg: Color,
    val heroOnPrimary: Color,
    val heroSubtleBg: Color,
    val navBarFill: Color,
    val sheetBg: Color,
    val sheetScrim: Color,
    val sheetCardBg: Color,
    // 5 Happy Solid Multi-Accents (Pesisir + Command Center)
    val accentCobalt: Color,
    val onAccentCobalt: Color,
    val accentCoral: Color,
    val onAccentCoral: Color,
    val accentOrange: Color,
    val onAccentOrange: Color,
    val accentAmber: Color,
    val onAccentAmber: Color,
    val accentMint: Color,
    val onAccentMint: Color,
    val accentSky: Color,
    // Extended surface & decorative tokens so secondary screens never hardcode hex colors
    val surfaceSubtle: Color,
    val surfaceMuted: Color,
    val warmTintBg: Color,
    val notebookHeaderBg: Color,
    val notebookPageBg: Color,
    val notebookLipBg: Color,
    val notebookBorder: Color,
    val notebookRuledLine: Color,
    val notebookBadgeBg: Color,
    val notebookBadgeFg: Color,
    val notebookBadgeBorder: Color,
    val notebookRing: Color,
    val notebookRingSheen: Color,
    val notebookHoleBg: Color,
    val overlayScrimBg: Color,
    val overlayScrimText: Color,
    val overlayAccentSoft: Color,
) {
    fun tabAccent(tab: MainTab): Pair<Color, Color> = when (tab) {
        MainTab.JADWAL -> accentCobalt to onAccentCobalt
        MainTab.KALENDER -> accentSky to onAccentCobalt
        MainTab.MATRIKS -> accentCoral to onAccentCoral
        MainTab.FOKUS -> accentOrange to onAccentOrange
        MainTab.REKAP -> accentMint to onAccentMint
        MainTab.ANALITIK -> accentAmber to onAccentAmber
    }
}

val LightAtmospherePalette = AtmospherePalette(
    isDark = false,
    bgTop = Color(0xFFF5F7FA),
    cardBg = Color(0xFFFFFFFF),
    cardAltBg = Color(0xFFEDF1F7),
    cardBorder = Color(0xFFE2E8F0),
    heroSolidBg = Color(0xFF1677FF),
    heroOnPrimary = Color(0xFFFFFFFF),
    heroSubtleBg = Color(0x2EFFFFFF),
    navBarFill = Color(0xFFFFFFFF),
    sheetBg = Color(0xFFFFFFFF),
    sheetScrim = Color(0x8A0F172A),
    sheetCardBg = Color(0xFFF4F6FA),
    accentCobalt = Color(0xFF1677FF),
    onAccentCobalt = Color(0xFFFFFFFF),
    accentCoral = Color(0xFFEE3E38),
    onAccentCoral = Color(0xFFFFFFFF),
    accentOrange = Color(0xFFF97316),
    onAccentOrange = Color(0xFFFFFFFF),
    accentAmber = Color(0xFFFACC15),
    onAccentAmber = Color(0xFF18181B),
    accentMint = Color(0xFF10B981),
    onAccentMint = Color(0xFFFFFFFF),
    accentSky = Color(0xFF0EA5E9),
    surfaceSubtle = Color(0xFFEEF2F6),
    surfaceMuted = Color(0xFFE2E8F0),
    warmTintBg = Color(0xFFFFF1E6),
    notebookHeaderBg = Color(0xFFE0ECFF),
    notebookPageBg = Color(0xFFF6FAFF),
    notebookLipBg = Color(0xFFD6E4FF),
    notebookBorder = Color(0xFFB3CDFF),
    notebookRuledLine = Color(0x381677FF),
    notebookBadgeBg = Color(0xFFE0ECFF),
    notebookBadgeFg = Color(0xFF1E40AF),
    notebookBadgeBorder = Color(0xFFB3CDFF),
    notebookRing = Color(0xFF1677FF),
    notebookRingSheen = Color(0xFFEFF6FF),
    notebookHoleBg = Color(0xFFCBD5E1),
    overlayScrimBg = Color(0xB8080D21),
    overlayScrimText = Color(0xFFBAE6FD),
    overlayAccentSoft = Color(0xFF93C5FD),
)

val DarkAtmospherePalette = AtmospherePalette(
    isDark = true,
    bgTop = Color(0xFF0E121B),
    cardBg = Color(0xFF1B212E),
    cardAltBg = Color(0xFF262E3F),
    cardBorder = Color(0xFF2D374B),
    heroSolidBg = Color(0xFF1D4ED8),
    heroOnPrimary = Color(0xFFFFFFFF),
    heroSubtleBg = Color(0x2BFFFFFF),
    navBarFill = Color(0xFF1B212E),
    sheetBg = Color(0xFF161B27),
    sheetScrim = Color(0xBA050811),
    sheetCardBg = Color(0xFF22293A),
    accentCobalt = Color(0xFF3B82F6),
    onAccentCobalt = Color(0xFFFFFFFF),
    accentCoral = Color(0xFFFB5A54),
    onAccentCoral = Color(0xFFFFFFFF),
    accentOrange = Color(0xFFFB923C),
    onAccentOrange = Color(0xFF1C1005),
    accentAmber = Color(0xFFFACC15),
    onAccentAmber = Color(0xFF18181B),
    accentMint = Color(0xFF34D399),
    onAccentMint = Color(0xFF052E21),
    accentSky = Color(0xFF38BDF8),
    surfaceSubtle = Color(0xFF171D2B),
    surfaceMuted = Color(0xFF252E42),
    warmTintBg = Color(0xFF332018),
    notebookHeaderBg = Color(0xFF28334E),
    notebookPageBg = Color(0xFF1F273B),
    notebookLipBg = Color(0xFF161D2E),
    notebookBorder = Color(0xFF4F6089),
    notebookRuledLine = Color(0x3860A5FA),
    notebookBadgeBg = Color(0xFF3B2E14),
    notebookBadgeFg = Color(0xFFFDE68A),
    notebookBadgeBorder = Color(0xFF6B5324),
    notebookRing = Color(0xFF93C5FD),
    notebookRingSheen = Color(0xFFDBEAFE),
    notebookHoleBg = Color(0xFF0E121B),
    overlayScrimBg = Color(0xCC0E121B),
    overlayScrimText = Color(0xFFBAE6FD),
    overlayAccentSoft = Color(0xFF93C5FD),
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
    greenBg = Color(0xFFD1FAE5),
    greenFg = Color(0xFF065F46),
    greenAccent = Color(0xFF10B981),
    yellowBg = Color(0xFFFFEDD5),
    yellowFg = Color(0xFF9A3412),
    yellowAccent = Color(0xFFF97316),
    redBg = Color(0xFFFEE2E2),
    redFg = Color(0xFF991B1B),
    redAccent = Color(0xFFEE3E38),
    grayBg = Color(0xFFF1F5F9),
    grayFg = Color(0xFF334155),
    grayAccent = Color(0xFF64748B),
    darkBg = Color(0xFF1E293B),
    darkFg = Color(0xFFFFFFFF),
    darkAccent = Color(0xFF0F172A),
    blueBg = Color(0xFFDBEAFE),
    blueFg = Color(0xFF1E40AF),
    blueAccent = Color(0xFF1677FF),
)

val DarkStatusPalette = SemanticStatusPalette(
    greenBg = Color(0xFF123F33),
    greenFg = Color(0xFF6EE7B7),
    greenAccent = Color(0xFF34D399),
    yellowBg = Color(0xFF4A250E),
    yellowFg = Color(0xFFFDBA74),
    yellowAccent = Color(0xFFFB923C),
    redBg = Color(0xFF4B1A1D),
    redFg = Color(0xFFFCA5A5),
    redAccent = Color(0xFFFB5A54),
    grayBg = Color(0xFF262E3F),
    grayFg = Color(0xFFE2E8F0),
    grayAccent = Color(0xFF94A3B8),
    darkBg = Color(0xFF361D29),
    darkFg = Color(0xFFFECDD3),
    darkAccent = Color(0xFFF43F5E),
    blueBg = Color(0xFF1A335C),
    blueFg = Color(0xFF93C5FD),
    blueAccent = Color(0xFF3B82F6),
)

val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }
val LocalAtmospherePalette = staticCompositionLocalOf { LightAtmospherePalette }
