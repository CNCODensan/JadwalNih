package com.example.ui.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.HolidayThemeType
import com.example.domain.PersonalBirthdayStats
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

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
fun AppThemeWallpaperLayer(
    holidayThemeType: HolidayThemeType,
    isBirthdayTheme: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val wallpaperRes = when (holidayThemeType) {
        HolidayThemeType.INDEPENDENCE_DAY -> R.drawable.img_bg_kemerdekaan
        HolidayThemeType.IDUL_FITRI -> R.drawable.img_bg_idul_fitri
        HolidayThemeType.NATAL -> R.drawable.img_bg_natal
        HolidayThemeType.IMLEK -> R.drawable.img_bg_imlek
        HolidayThemeType.NYEPI_WAISAK -> R.drawable.img_bg_nyepi_waisak
        HolidayThemeType.NONE -> null
    }

    if (wallpaperRes != null) {
        Box(modifier = modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = wallpaperRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (isDark) 0.36f else 0.48f,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                atmo.bgTop.copy(alpha = if (isDark) 0.56f else 0.36f),
                                atmo.bgTop.copy(alpha = if (isDark) 0.38f else 0.22f),
                                atmo.bgTop.copy(alpha = if (isDark) 0.64f else 0.45f),
                            ),
                        ),
                    ),
            )
        }
    } else if (isBirthdayTheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF28102D), Color(0xFF19101E), Color(0xFF24132C))
                        } else {
                            listOf(Color(0xFFFFEAF4), Color(0xFFFFF7FB), Color(0xFFFEF3C7).copy(alpha = 0.7f))
                        },
                    ),
                ),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val colors = listOf(
                    Color(0xFFF472B6).copy(alpha = 0.20f),
                    Color(0xFFFBBF24).copy(alpha = 0.20f),
                    Color(0xFF60A5FA).copy(alpha = 0.18f),
                    Color(0xFF34D399).copy(alpha = 0.18f),
                )
                drawCircle(color = colors[0], radius = w * 0.28f, center = Offset(w * 0.12f, h * 0.18f))
                drawCircle(color = colors[1], radius = w * 0.24f, center = Offset(w * 0.88f, h * 0.26f))
                drawCircle(color = colors[2], radius = w * 0.26f, center = Offset(w * 0.20f, h * 0.78f))
                drawCircle(color = colors[3], radius = w * 0.22f, center = Offset(w * 0.85f, h * 0.82f))
            }
        }
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

/**
 * Plays an expressive, stylized once-per-open holiday motion graphic overlay (~3.5s)
 * and lets the user skip/tap anywhere to dismiss immediately.
 */
@Composable
fun HolidayMotionOverlay(
    themeType: HolidayThemeType?,
    onDismiss: () -> Unit,
) {
    if (themeType == null || themeType == HolidayThemeType.NONE) return

    BackHandler { onDismiss() }

    val progress = remember(themeType) { Animatable(0f) }
    LaunchedEffect(themeType) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3600, easing = LinearEasing),
        )
        delay(250L)
        onDismiss()
    }

    val t = progress.value
    val overlayAlpha = when {
        t < 0.10f -> t / 0.10f
        t > 0.85f -> (1f - t) / 0.15f
        else -> 1f
    }.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f * overlayAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDismiss() }
            .testTag("holiday_motion_overlay"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (themeType) {
                HolidayThemeType.INDEPENDENCE_DAY -> {
                    // Waving Merah-Putih flag + simple playing kids & hero figure
                    val poleX = w * 0.22f
                    val poleTop = h * 0.24f
                    val poleBottom = h * 0.64f
                    drawLine(
                        color = Color(0xFFE5E7EB).copy(alpha = overlayAlpha),
                        start = Offset(poleX, poleTop),
                        end = Offset(poleX, poleBottom),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = overlayAlpha),
                        radius = 12f,
                        center = Offset(poleX, poleTop),
                    )
                    val flagW = w * 0.44f
                    val stripeH = 44f
                    val segments = 16
                    for (seg in 0 until segments) {
                        val x0 = poleX + 8f + (flagW * seg / segments)
                        val segW = (flagW / segments) + 2f
                        val wave = sin((t * 14f) + seg * 0.55f) * 14f * ((seg + 1f) / segments)
                        drawRect(
                            color = Color(0xFFDC2626).copy(alpha = overlayAlpha),
                            topLeft = Offset(x0, poleTop + 10f + wave),
                            size = Size(segW, stripeH),
                        )
                        drawRect(
                            color = Color.White.copy(alpha = overlayAlpha),
                            topLeft = Offset(x0, poleTop + 10f + stripeH + wave),
                            size = Size(segW, stripeH),
                        )
                    }
                    // Hero silhouette raising hand on the right
                    val heroX = w * 0.72f
                    val groundY = h * 0.64f
                    val heroColor = Color(0xFF991B1B).copy(alpha = overlayAlpha)
                    drawCircle(heroColor, radius = 20f, center = Offset(heroX, groundY - 94f))
                    drawRoundRect(
                        color = heroColor,
                        topLeft = Offset(heroX - 18f, groundY - 72f),
                        size = Size(36f, 52f),
                        cornerRadius = CornerRadius(14f, 14f),
                    )
                    drawLine(heroColor, Offset(heroX - 10f, groundY - 22f), Offset(heroX - 16f, groundY + 12f), strokeWidth = 12f, cap = StrokeCap.Round)
                    drawLine(heroColor, Offset(heroX + 10f, groundY - 22f), Offset(heroX + 16f, groundY + 12f), strokeWidth = 12f, cap = StrokeCap.Round)
                    // Raised arm + small headband
                    drawLine(heroColor, Offset(heroX + 12f, groundY - 64f), Offset(heroX + 42f, groundY - 104f), strokeWidth = 10f, cap = StrokeCap.Round)
                    drawRect(Color(0xFFEF4444).copy(alpha = overlayAlpha), topLeft = Offset(heroX - 20f, groundY - 102f), size = Size(40f, 8f))

                    // Two simple playing kids bouncing cheerfully
                    for (k in 0..1) {
                        val kidX = w * (0.36f + k * 0.16f) + sin(t * 9f + k * 2f) * 14f
                        val bounce = kotlin.math.abs(sin(t * 10f + k * 1.4f)) * 26f
                        val kidY = groundY - bounce
                        val kidTone = (if (k == 0) Color(0xFFEF4444) else Color(0xFFF59E0B)).copy(alpha = overlayAlpha)
                        drawCircle(Color(0xFFFEF3C7).copy(alpha = overlayAlpha), radius = 14f, center = Offset(kidX, kidY - 56f))
                        drawRoundRect(kidTone, topLeft = Offset(kidX - 12f, kidY - 40f), size = Size(24f, 34f), cornerRadius = CornerRadius(10f, 10f))
                        drawLine(Color.White.copy(alpha = overlayAlpha), Offset(kidX + 12f, kidY - 34f), Offset(kidX + 26f, kidY - 56f), strokeWidth = 5f)
                        drawRect(Color(0xFFDC2626).copy(alpha = overlayAlpha), topLeft = Offset(kidX + 26f, kidY - 68f), size = Size(20f, 12f))
                    }
                }

                HolidayThemeType.NATAL -> {
                    // Drifting snowflakes + glowing Christmas star
                    val flakes = 36
                    for (i in 0 until flakes) {
                        val seed = i * 73.1f
                        val fx = ((seed % w) + sin(t * 5f + i) * 24f + w) % w
                        val fy = ((t * (h * 0.85f) + (i * h * 0.09f)) % h)
                        val r = 4f + (i % 4) * 3.5f
                        drawCircle(
                            color = Color.White.copy(alpha = overlayAlpha * 0.85f),
                            radius = r,
                            center = Offset(fx, fy),
                        )
                    }
                    // Pine & star silhouette center-bottom
                    val treeCx = w * 0.5f
                    val treeTop = h * 0.34f
                    val treePath = Path().apply {
                        moveTo(treeCx, treeTop)
                        lineTo(treeCx + w * 0.24f, h * 0.64f)
                        lineTo(treeCx - w * 0.24f, h * 0.64f)
                        close()
                    }
                    drawPath(treePath, color = Color(0xFF15803D).copy(alpha = overlayAlpha * 0.9f))
                    drawCircle(Color(0xFFFACC15).copy(alpha = overlayAlpha), radius = 18f, center = Offset(treeCx, treeTop - 6f))
                }

                HolidayThemeType.IDUL_FITRI -> {
                    // Animated mosque dome + minarets rising + glowing crescent moon
                    val rise = (1f - (t * 2.2f).coerceIn(0f, 1f)) * 90f
                    val baseY = h * 0.64f + rise
                    val emerald = Color(0xFF065F46).copy(alpha = overlayAlpha * 0.94f)
                    val gold = Color(0xFFFBBF24).copy(alpha = overlayAlpha)

                    // Crescent moon top right
                    drawCircle(gold, radius = 44f, center = Offset(w * 0.76f, h * 0.24f))
                    drawCircle(Color(0xFF0A1D16).copy(alpha = overlayAlpha), radius = 36f, center = Offset(w * 0.80f, h * 0.22f))

                    // Central dome + building + minarets
                    drawArc(
                        color = emerald,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(w * 0.32f, baseY - 220f),
                        size = Size(w * 0.36f, 220f),
                    )
                    drawRect(
                        color = emerald,
                        topLeft = Offset(w * 0.26f, baseY - 110f),
                        size = Size(w * 0.48f, 110f),
                    )
                    // Left & right minarets
                    drawRect(emerald, topLeft = Offset(w * 0.16f, baseY - 240f), size = Size(34f, 240f))
                    drawRect(emerald, topLeft = Offset(w * 0.84f - 34f, baseY - 240f), size = Size(34f, 240f))
                    // Warm lit arches + finial
                    drawCircle(gold, radius = 10f, center = Offset(w * 0.5f, baseY - 235f))
                    drawRoundRect(
                        color = gold.copy(alpha = overlayAlpha * 0.85f),
                        topLeft = Offset(w * 0.46f, baseY - 72f),
                        size = Size(w * 0.08f, 72f),
                        cornerRadius = CornerRadius(24f, 24f),
                    )
                }

                HolidayThemeType.IMLEK -> {
                    // Chinese dragon undulating across screen
                    val headX = w * (1.15f - t * 1.45f)
                    val baseY = h * 0.42f
                    val segments = 22
                    for (i in segments downTo 0) {
                        val sx = headX + i * 24f
                        val sy = baseY + sin(t * 16f - i * 0.45f) * 44f
                        val rad = (28f - i * 0.8f).coerceAtLeast(10f)
                        val color = if (i % 2 == 0) Color(0xFFDC2626) else Color(0xFFFBBF24)
                        drawCircle(
                            color = color.copy(alpha = overlayAlpha),
                            radius = rad,
                            center = Offset(sx, sy),
                        )
                    }
                    // Dragon head + glowing pearl ahead
                    val headY = baseY + sin(t * 16f) * 44f
                    drawCircle(Color(0xFFEF4444).copy(alpha = overlayAlpha), radius = 34f, center = Offset(headX, headY))
                    drawCircle(Color.White.copy(alpha = overlayAlpha), radius = 7f, center = Offset(headX - 10f, headY - 8f))
                    drawCircle(Color(0xFFFDE047).copy(alpha = overlayAlpha), radius = 14f, center = Offset(headX - 74f, headY - 12f))
                }

                HolidayThemeType.NYEPI_WAISAK -> {
                    // Floating lanterns rising softly
                    for (i in 0..8) {
                        val lx = w * (0.12f + (i * 0.1f)) + sin(t * 4f + i) * 18f
                        val ly = h * (0.72f - t * 0.45f) - (i % 3) * 60f
                        drawRoundRect(
                            color = Color(0xFFFBBF24).copy(alpha = overlayAlpha * 0.9f),
                            topLeft = Offset(lx - 18f, ly - 24f),
                            size = Size(36f, 46f),
                            cornerRadius = CornerRadius(12f, 12f),
                        )
                    }
                }

                HolidayThemeType.NONE -> Unit
            }
        }

        // Caption pill at bottom center + Skip button
        Surface(
            color = Color(0xFF111827).copy(alpha = 0.78f * overlayAlpha),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${themeType.labelId} • Ketuk untuk lewati",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Full-screen user birthday celebration with animated Canvas cake + flickering candles,
 * floating balloons & confetti, congratulations + thank-you card, and JadwalNih usage stats.
 */
@Composable
fun BirthdayCelebrationOverlay(
    userName: String,
    stats: PersonalBirthdayStats,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }

    val atmo = LocalAtmospherePalette.current
    val infinite = rememberInfiniteTransition(label = "bday_anim")
    val flicker by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flame_flicker",
    )
    val floatProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "balloon_float",
    )

    val displayName = userName.trim().ifBlank { "Sahabat JadwalNih" }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(220)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1F1033).copy(alpha = 0.94f),
                            Color(0xFF4A1037).copy(alpha = 0.94f),
                            Color(0xFF1E1B4B).copy(alpha = 0.96f),
                        ),
                    ),
                )
                .testTag("birthday_celebration_overlay"),
        ) {
            // Animated Balloons & Confetti Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val balloonColors = listOf(
                    Color(0xFFF472B6),
                    Color(0xFFFBBF24),
                    Color(0xFF38BDF8),
                    Color(0xFF34D399),
                    Color(0xFFFB7185),
                )
                for (i in 0 until 7) {
                    val bx = w * (0.10f + i * 0.13f) + sin(floatProgress * 2 * PI + i).toFloat() * 14f
                    val by = h - ((floatProgress * h * 1.15f + i * 160f) % (h * 1.15f))
                    val c = balloonColors[i % balloonColors.size]
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f),
                        start = Offset(bx, by + 32f),
                        end = Offset(bx + 4f, by + 84f),
                        strokeWidth = 2f,
                    )
                    drawOval(
                        color = c.copy(alpha = 0.85f),
                        topLeft = Offset(bx - 22f, by - 28f),
                        size = Size(44f, 56f),
                    )
                }
                for (k in 0 until 26) {
                    val cx = ((k * 97f) % w)
                    val cy = ((floatProgress * h + k * 79f) % h)
                    drawCircle(
                        color = balloonColors[k % balloonColors.size].copy(alpha = 0.72f),
                        radius = 4f + (k % 3) * 2.5f,
                        center = Offset(cx, cy),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(48.dp)
                            .clickable { onDismiss() }
                            .testTag("birthday_overlay_top_close"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup ucapan ulang tahun", tint = Color.White)
                        }
                    }
                }

                // Animated Layered Birthday Cake with Flickering Candles
                Canvas(
                    modifier = Modifier
                        .size(width = 180.dp, height = 148.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    // Plate
                    drawRoundRect(
                        color = Color(0xFFE2E8F0),
                        topLeft = Offset(w * 0.10f, h * 0.86f),
                        size = Size(w * 0.80f, 14f),
                        cornerRadius = CornerRadius(7f, 7f),
                    )
                    // Bottom tier
                    drawRoundRect(
                        color = Color(0xFFF472B6),
                        topLeft = Offset(w * 0.16f, h * 0.56f),
                        size = Size(w * 0.68f, h * 0.30f),
                        cornerRadius = CornerRadius(14f, 14f),
                    )
                    // Cream frosting strip
                    drawRoundRect(
                        color = Color(0xFFFFFBEB),
                        topLeft = Offset(w * 0.16f, h * 0.56f),
                        size = Size(w * 0.68f, 16f),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                    // Top tier
                    drawRoundRect(
                        color = Color(0xFFFBBF24),
                        topLeft = Offset(w * 0.27f, h * 0.33f),
                        size = Size(w * 0.46f, h * 0.23f),
                        cornerRadius = CornerRadius(12f, 12f),
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.27f, h * 0.33f),
                        size = Size(w * 0.46f, 14f),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                    // 3 Candles + flickering flames
                    val candleXs = listOf(w * 0.38f, w * 0.50f, w * 0.62f)
                    candleXs.forEachIndexed { idx, cx ->
                        val candleColor = listOf(Color(0xFF38BDF8), Color(0xFFFB7185), Color(0xFF34D399))[idx]
                        drawRoundRect(
                            color = candleColor,
                            topLeft = Offset(cx - 5f, h * 0.17f),
                            size = Size(10f, h * 0.16f),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                        // Outer glow
                        drawCircle(
                            color = Color(0xFFFDE047).copy(alpha = 0.40f),
                            radius = 12f * flicker,
                            center = Offset(cx, h * 0.12f),
                        )
                        // Inner flame
                        drawOval(
                            color = Color(0xFFF97316),
                            topLeft = Offset(cx - 5f * flicker, h * 0.08f),
                            size = Size(10f * flicker, 16f * flicker),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Surface(
                    color = atmo.cardBg.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFF472B6).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Filled.Cake, contentDescription = null, tint = Color(0xFFDB2777))
                            Text(
                                text = "SELAMAT ULANG TAHUN!",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFFDB2777),
                            )
                        }
                        Text(
                            text = "Selamat ulang tahun, $displayName!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Terima kasih sudah mempercayakan harimu bersama JadwalNih selama ${stats.usageDurationLabel}. Semoga sehat selalu, penuh berkah, dan makin banyak rencana indah yang terwujud!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        // Personal Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BirthdayStatTile(
                                value = stats.usageDurationLabel,
                                label = "Pakai JadwalNih",
                                modifier = Modifier.weight(1f),
                            )
                            BirthdayStatTile(
                                value = "${stats.totalCompletedTasks}",
                                label = "Jadwal Beres",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BirthdayStatTile(
                                value = "${stats.totalFocusMinutes} mnt",
                                label = "Total Fokus",
                                modifier = Modifier.weight(1f),
                            )
                            BirthdayStatTile(
                                value = "${stats.totalPhotoRecaps} foto • ${stats.streakDays}h",
                                label = "Rekap & Streak",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDB2777),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("birthday_celebration_close_button"),
                        ) {
                            Text(
                                text = "Terima Kasih! Lanjut Beraktivitas",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BirthdayStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    Surface(
        color = atmo.surfaceSubtle,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

