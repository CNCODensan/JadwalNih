package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.HolidayThemeType

val LocalIsBirthdayTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
)

private val JadwalNihShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun JadwalNihTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    holidayThemeType: HolidayThemeType = HolidayThemeType.NONE,
    isBirthdayTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = if (darkTheme) DarkColors else LightColors
    val holidayColored = applyHolidayColorScheme(baseColorScheme, holidayThemeType, darkTheme)
    val colorScheme = if (isBirthdayTheme && holidayThemeType == HolidayThemeType.NONE) {
        if (darkTheme) {
            holidayColored.copy(
                primary = Color(0xFFF472B6),
                onPrimary = Color(0xFF4A042E),
                primaryContainer = Color(0xFF6B1547),
                onPrimaryContainer = Color(0xFFFCE7F3),
                secondary = Color(0xFFFBBF24),
            )
        } else {
            holidayColored.copy(
                primary = Color(0xFFDB2777),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFCE7F3),
                onPrimaryContainer = Color(0xFF831843),
                secondary = Color(0xFFF59E0B),
            )
        }
    } else {
        holidayColored
    }

    val statusPalette = if (darkTheme) DarkStatusPalette else LightStatusPalette
    val baseAtmosphere = if (darkTheme) DarkAtmospherePalette else LightAtmospherePalette
    val holidayAtmosphere = applyHolidayAtmosphere(baseAtmosphere, holidayThemeType)
    val birthdayAtmosphere = if (isBirthdayTheme && holidayThemeType == HolidayThemeType.NONE) {
        holidayAtmosphere.copy(
            bgTop = if (darkTheme) Color(0xFF19101E) else Color(0xFFFFF5FA),
            heroSolidBg = if (darkTheme) Color(0xFF9D174D) else Color(0xFFDB2777),
            accentCobalt = if (darkTheme) Color(0xFFF472B6) else Color(0xFFDB2777),
            accentAmber = Color(0xFFF59E0B),
            cardBorder = if (darkTheme) Color(0xFF53223F) else Color(0xFFFBCFE8),
            warmTintBg = if (darkTheme) Color(0xFF2D1426) else Color(0xFFFDF2F8),
        )
    } else {
        holidayAtmosphere
    }

    val hasWallpaper = holidayThemeType != HolidayThemeType.NONE || isBirthdayTheme
    val atmospherePalette = if (hasWallpaper) {
        val cardAlpha = if (darkTheme) 0.84f else 0.86f
        val altAlpha = if (darkTheme) 0.80f else 0.84f
        birthdayAtmosphere.copy(
            cardBg = birthdayAtmosphere.cardBg.copy(alpha = cardAlpha),
            cardAltBg = birthdayAtmosphere.cardAltBg.copy(alpha = altAlpha),
            navBarFill = birthdayAtmosphere.navBarFill.copy(alpha = 0.92f),
        )
    } else {
        birthdayAtmosphere
    }

    CompositionLocalProvider(
        LocalStatusPalette provides statusPalette,
        LocalAtmospherePalette provides atmospherePalette,
        LocalHolidayThemeType provides holidayThemeType,
        LocalIsBirthdayTheme provides isBirthdayTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JadwalNihTypography,
            shapes = JadwalNihShapes,
            content = content,
        )
    }
}
