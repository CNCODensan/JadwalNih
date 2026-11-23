package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.domain.HolidayThemeType
import kotlin.math.cos
import kotlin.math.sin

val LocalHolidayThemeType = staticCompositionLocalOf { HolidayThemeType.NONE }

fun applyHolidayColorScheme(
    base: ColorScheme,
    themeType: HolidayThemeType,
    isDark: Boolean,
): ColorScheme {
    return when (themeType) {
        HolidayThemeType.NONE -> base
        HolidayThemeType.INDEPENDENCE_DAY -> if (isDark) {
            base.copy(
                primary = Color(0xFFEF4444),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF5C1515),
                onPrimaryContainer = Color(0xFFFEE2E2),
            )
        } else {
            base.copy(
                primary = Color(0xFFD32F2F),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFEBEE),
                onPrimaryContainer = Color(0xFF8E1111),
            )
        }
        HolidayThemeType.IDUL_FITRI -> if (isDark) {
            base.copy(
                primary = Color(0xFF10B981),
                onPrimary = Color(0xFF042F22),
                primaryContainer = Color(0xFF064E3B),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondary = Color(0xFFFBBF24),
            )
        } else {
            base.copy(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF064E3B),
                secondary = Color(0xFFD97706),
            )
        }
        HolidayThemeType.IMLEK -> if (isDark) {
            base.copy(
                primary = Color(0xFFF87171),
                onPrimary = Color(0xFF450A0A),
                primaryContainer = Color(0xFF7F1D1D),
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondary = Color(0xFFFBBF24),
            )
        } else {
            base.copy(
                primary = Color(0xFFB91C1C),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFEF3C7),
                onPrimaryContainer = Color(0xFF92400E),
                secondary = Color(0xFFD97706),
            )
        }
        HolidayThemeType.NATAL -> if (isDark) {
            base.copy(
                primary = Color(0xFF22C55E),
                onPrimary = Color(0xFF052E16),
                primaryContainer = Color(0xFF14532D),
                onPrimaryContainer = Color(0xFFDCFCE7),
                tertiary = Color(0xFFFB7185),
            )
        } else {
            base.copy(
                primary = Color(0xFF15803D),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDCFCE7),
                onPrimaryContainer = Color(0xFF14532D),
                tertiary = Color(0xFFDC2626),
            )
        }
        HolidayThemeType.NYEPI_WAISAK -> if (isDark) {
            base.copy(
                primary = Color(0xFFA78BFA),
                onPrimary = Color(0xFF2E1065),
                primaryContainer = Color(0xFF4C1D95),
                onPrimaryContainer = Color(0xFFEDE9FE),
                secondary = Color(0xFFFBBF24),
            )
        } else {
            base.copy(
                primary = Color(0xFF6D28D9),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEDE9FE),
                onPrimaryContainer = Color(0xFF4C1D95),
                secondary = Color(0xFFD97706),
            )
        }
    }
}

fun applyHolidayAtmosphere(
    base: AtmospherePalette,
    themeType: HolidayThemeType,
): AtmospherePalette {
    val isDark = base.isDark
    return when (themeType) {
        HolidayThemeType.NONE -> base
        HolidayThemeType.INDEPENDENCE_DAY -> base.copy(
            bgTop = if (isDark) Color(0xFF181115) else Color(0xFFFFF8F8),
            heroSolidBg = if (isDark) Color(0xFFB91C1C) else Color(0xFFD32F2F),
            accentCobalt = if (isDark) Color(0xFFEF4444) else Color(0xFFD32F2F),
            cardBorder = if (isDark) Color(0xFF4C2327) else Color(0xFFFECACA),
            warmTintBg = if (isDark) Color(0xFF2B1316) else Color(0xFFFFEDED),
        )
        HolidayThemeType.IDUL_FITRI -> base.copy(
            bgTop = if (isDark) Color(0xFF0C1814) else Color(0xFFF2FBF7),
            heroSolidBg = if (isDark) Color(0xFF065F46) else Color(0xFF047857),
            accentCobalt = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
            accentAmber = Color(0xFFF59E0B),
            cardBorder = if (isDark) Color(0xFF1E4438) else Color(0xFFA7F3D0),
            warmTintBg = if (isDark) Color(0xFF102A22) else Color(0xFFECFDF5),
        )
        HolidayThemeType.IMLEK -> base.copy(
            bgTop = if (isDark) Color(0xFF1A1012) else Color(0xFFFFF8EE),
            heroSolidBg = if (isDark) Color(0xFF991B1B) else Color(0xFFC62828),
            accentCobalt = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
            accentAmber = Color(0xFFF59E0B),
            cardBorder = if (isDark) Color(0xFF532424) else Color(0xFFFDE68A),
            warmTintBg = if (isDark) Color(0xFF2D1414) else Color(0xFFFEF3C7),
        )
        HolidayThemeType.NATAL -> base.copy(
            bgTop = if (isDark) Color(0xFF0E1713) else Color(0xFFF3FAF5),
            heroSolidBg = if (isDark) Color(0xFF14532D) else Color(0xFF15803D),
            accentCobalt = if (isDark) Color(0xFF22C55E) else Color(0xFF15803D),
            cardBorder = if (isDark) Color(0xFF1E402B) else Color(0xFFBBF7D0),
            warmTintBg = if (isDark) Color(0xFF291418) else Color(0xFFFFF1F2),
        )
        HolidayThemeType.NYEPI_WAISAK -> base.copy(
            bgTop = if (isDark) Color(0xFF141122) else Color(0xFFF8F6FF),
            heroSolidBg = if (isDark) Color(0xFF5B21B6) else Color(0xFF6D28D9),
            accentCobalt = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
            cardBorder = if (isDark) Color(0xFF36285A) else Color(0xFFDDD6FE),
            warmTintBg = if (isDark) Color(0xFF211B36) else Color(0xFFF3E8FF),
        )
    }
}

@Composable
fun HolidayHeaderOrnament(
    themeType: HolidayThemeType,
    modifier: Modifier = Modifier,
) {
    if (themeType == HolidayThemeType.NONE) return
    Canvas(modifier = modifier.size(34.dp)) {
        val w = size.width
        val h = size.height
        when (themeType) {
            HolidayThemeType.INDEPENDENCE_DAY -> {
                // Flag pole + waving Merah-Putih flag
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(w * 0.2f, h * 0.14f),
                    end = Offset(w * 0.2f, h * 0.88f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
                drawRoundRect(
                    color = Color(0xFFDC2626),
                    topLeft = Offset(w * 0.24f, h * 0.16f),
                    size = Size(w * 0.62f, h * 0.24f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.24f, h * 0.40f),
                    size = Size(w * 0.62f, h * 0.24f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
            HolidayThemeType.IDUL_FITRI -> {
                // Gold Crescent + Ketupat Diamond
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = w * 0.28f,
                    center = Offset(w * 0.44f, h * 0.50f),
                )
                drawCircle(
                    color = Color(0xFF047857),
                    radius = w * 0.22f,
                    center = Offset(w * 0.53f, h * 0.44f),
                )
                val star = Offset(w * 0.72f, h * 0.34f)
                drawCircle(color = Color(0xFFFDE68A), radius = 3.5f, center = star)
            }
            HolidayThemeType.IMLEK -> {
                // Festive Red & Gold Lantern
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = Offset(w * 0.5f, h * 0.08f),
                    end = Offset(w * 0.5f, h * 0.22f),
                    strokeWidth = 2.5f,
                )
                drawOval(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(w * 0.22f, h * 0.22f),
                    size = Size(w * 0.56f, h * 0.48f),
                )
                drawRoundRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(w * 0.35f, h * 0.19f),
                    size = Size(w * 0.30f, h * 0.08f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
                drawRoundRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(w * 0.35f, h * 0.67f),
                    size = Size(w * 0.30f, h * 0.08f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = Offset(w * 0.5f, h * 0.75f),
                    end = Offset(w * 0.5f, h * 0.92f),
                    strokeWidth = 3f,
                )
            }
            HolidayThemeType.NATAL -> {
                // Pine tree + Star
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.80f, h * 0.74f)
                    lineTo(w * 0.20f, h * 0.74f)
                    close()
                }
                drawPath(path, color = Color(0xFF22C55E))
                drawRect(
                    color = Color(0xFF92400E),
                    topLeft = Offset(w * 0.42f, h * 0.74f),
                    size = Size(w * 0.16f, h * 0.14f),
                )
                drawCircle(color = Color(0xFFFACC15), radius = 4.5f, center = Offset(w * 0.5f, h * 0.14f))
                drawCircle(color = Color(0xFFEF4444), radius = 3f, center = Offset(w * 0.44f, h * 0.46f))
                drawCircle(color = Color(0xFFFACC15), radius = 3f, center = Offset(w * 0.58f, h * 0.58f))
            }
            HolidayThemeType.NYEPI_WAISAK -> {
                // Peaceful Lotus Circles
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                    radius = w * 0.18f,
                    center = Offset(w * 0.5f, h * 0.50f),
                )
                drawCircle(
                    color = Color(0xFFDDD6FE),
                    radius = w * 0.34f,
                    center = Offset(w * 0.5f, h * 0.50f),
                    style = Stroke(width = 2.5f),
                )
            }
            HolidayThemeType.NONE -> Unit
        }
    }
}

@Composable
fun HolidayScheduleWatermarkOverlay(
    themeType: HolidayThemeType,
    modifier: Modifier = Modifier,
) {
    if (themeType == HolidayThemeType.NONE) return
    val atmo = LocalAtmospherePalette.current
    val alpha = if (atmo.isDark) 0.085f else 0.07f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (themeType) {
            HolidayThemeType.INDEPENDENCE_DAY -> {
                // Subtle diagonal Merah-Putih ribbons + star emblems
                val red = Color(0xFFEF4444).copy(alpha = alpha)
                val white = (if (atmo.isDark) Color.White else Color(0xFFE11D48)).copy(alpha = alpha * 0.7f)
                drawCircle(color = red, radius = w * 0.24f, center = Offset(w * 0.88f, h * 0.14f))
                drawCircle(color = white, radius = w * 0.14f, center = Offset(w * 0.88f, h * 0.14f))
                drawRoundRect(
                    color = red,
                    topLeft = Offset(w * 0.06f, h * 0.58f),
                    size = Size(w * 0.28f, 34f),
                    cornerRadius = CornerRadius(12f, 12f),
                )
                drawRoundRect(
                    color = white,
                    topLeft = Offset(w * 0.06f, h * 0.58f + 34f),
                    size = Size(w * 0.28f, 34f),
                    cornerRadius = CornerRadius(12f, 12f),
                )
            }
            HolidayThemeType.IDUL_FITRI -> {
                // Faint Ketupat lattice diamond + crescent moon
                val emerald = Color(0xFF10B981).copy(alpha = alpha)
                val gold = Color(0xFFF59E0B).copy(alpha = alpha)
                val cx = w * 0.84f
                val cy = h * 0.18f
                val d = w * 0.14f
                val diamond = Path().apply {
                    moveTo(cx, cy - d)
                    lineTo(cx + d, cy)
                    lineTo(cx, cy + d)
                    lineTo(cx - d, cy)
                    close()
                }
                drawPath(diamond, color = emerald, style = Stroke(width = 6f))
                drawCircle(color = gold, radius = w * 0.16f, center = Offset(w * 0.14f, h * 0.68f))
            }
            HolidayThemeType.IMLEK -> {
                val red = Color(0xFFEF4444).copy(alpha = alpha)
                val gold = Color(0xFFF59E0B).copy(alpha = alpha)
                drawOval(
                    color = red,
                    topLeft = Offset(w * 0.72f, h * 0.08f),
                    size = Size(w * 0.20f, w * 0.24f),
                )
                drawCircle(color = gold, radius = w * 0.15f, center = Offset(w * 0.16f, h * 0.66f), style = Stroke(width = 8f))
            }
            HolidayThemeType.NATAL -> {
                val pine = Color(0xFF22C55E).copy(alpha = alpha)
                val berry = Color(0xFFEF4444).copy(alpha = alpha)
                for (k in 0..5) {
                    val angle = k * (Math.PI / 3.0)
                    val cx = w * 0.84f
                    val cy = h * 0.18f
                    val r = w * 0.14f
                    drawLine(
                        color = pine,
                        start = Offset(cx, cy),
                        end = Offset(cx + (r * cos(angle)).toFloat(), cy + (r * sin(angle)).toFloat()),
                        strokeWidth = 5f,
                    )
                }
                drawCircle(color = berry, radius = w * 0.10f, center = Offset(w * 0.14f, h * 0.65f))
            }
            HolidayThemeType.NYEPI_WAISAK -> {
                val violet = Color(0xFF8B5CF6).copy(alpha = alpha)
                val gold = Color(0xFFF59E0B).copy(alpha = alpha * 0.9f)
                drawCircle(color = violet, radius = w * 0.20f, center = Offset(w * 0.86f, h * 0.18f), style = Stroke(width = 6f))
                drawCircle(color = gold, radius = w * 0.12f, center = Offset(w * 0.86f, h * 0.18f), style = Stroke(width = 4f))
                drawCircle(color = violet, radius = w * 0.14f, center = Offset(w * 0.14f, h * 0.68f))
            }
            HolidayThemeType.NONE -> Unit
        }
    }
}
