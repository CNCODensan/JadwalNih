package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.DateTimeHelper
import com.example.domain.GreetingState
import com.example.domain.HolidayThemeType
import com.example.domain.QuickGlanceSummary
import com.example.domain.QuickGlanceTarget
import com.example.ui.theme.HolidayHeaderOrnament
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.MonoSmallStyle

@Composable
fun JadwalNihTopBar(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    holidayThemeType: HolidayThemeType = HolidayThemeType.NONE,
    unreadNotificationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
) {
    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val haptics = LocalHapticFeedback.current
    val settingsInteraction = remember { MutableInteractionSource() }
    val notifInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .padding(
                horizontal = spec.screenHorizontalPadding,
                vertical = if (spec.isUltraCompact) 8.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (spec.isUltraCompact) 8.dp else 10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_brand_icon),
                contentDescription = "Ikon JadwalNih",
                modifier = Modifier
                    .size(if (spec.isUltraCompact) 36.dp else 40.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "JadwalNih",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (21f * spec.titleScaleFactor).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    HolidayHeaderOrnament(themeType = holidayThemeType)
                }
                Text(
                    text = DateTimeHelper.formatIndonesianFullDate(DateTimeHelper.todayIso()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box {
                Surface(
                    shape = CircleShape,
                    color = atmo.cardBg,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, atmo.cardBorder),
                    modifier = Modifier.bouncyPress(notifInteraction, pressedScale = 0.86f),
                ) {
                    BouncyIconButton(
                        onClick = {
                            haptics.tapSoft()
                            onOpenNotifications()
                        },
                        interactionSource = notifInteraction,
                        pressedScale = 1f,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("notification_bell_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Buka Pusat Notifikasi",
                            tint = atmo.accentCobalt,
                        )
                    }
                }
                if (unreadNotificationCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("notification_unread_badge"),
                    ) {
                        Text(
                            text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = atmo.cardBg,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, atmo.cardBorder),
                modifier = Modifier.bouncyPress(settingsInteraction, pressedScale = 0.86f),
            ) {
                BouncyIconButton(
                    onClick = {
                        haptics.tapSoft()
                        onOpenSettings()
                    },
                    interactionSource = settingsInteraction,
                    pressedScale = 1f,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Buka Pengaturan",
                        tint = atmo.accentCobalt,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RingkasanCepatCard(
    summary: QuickGlanceSummary,
    greeting: GreetingState,
    tapTarget: QuickGlanceTarget,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false,
) {
    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(26.dp)
    val rawFraction = if (summary.todayTotalCount == 0) {
        0f
    } else {
        summary.todayCompletedCount.toFloat() / summary.todayTotalCount.toFloat()
    }
    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "hero_progress",
    )
    val animatedPercent by animateIntAsState(
        targetValue = (rawFraction * 100).toInt(),
        animationSpec = tween(durationMillis = 550),
        label = "hero_percent",
    )
    val cardInteraction = remember { MutableInteractionSource() }

    val heroTextBlock: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = greeting.greetingText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = atmo.heroOnPrimary.copy(alpha = 0.94f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    summary.todayTotalCount == 0 -> "Belum ada agenda"
                    summary.nextTaskTitle != null -> summary.nextTaskTitle
                    else -> "Hari ini beres!"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (16f * spec.titleScaleFactor).sp,
                ),
                color = atmo.heroOnPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = greeting.encouragementText,
                style = MaterialTheme.typography.bodySmall,
                color = atmo.heroOnPrimary.copy(alpha = 0.84f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    val heroChipsBlock: @Composable () -> Unit = {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                color = atmo.heroSubtleBg,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = when {
                        summary.todayTotalCount == 0 -> "0 agenda"
                        summary.nextTaskTimeLabel != null -> summary.nextTaskTimeLabel
                        else -> "${summary.todayActiveOrUpcomingCount} tersisa"
                    },
                    style = MonoSmallStyle,
                    color = atmo.heroOnPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
            if (summary.streakDays >= 1) {
                val streakScale = remember { Animatable(1f) }
                LaunchedEffect(summary.streakDays) {
                    streakScale.snapTo(1.12f)
                    streakScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                Surface(
                    color = atmo.accentAmber,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = streakScale.value
                        scaleY = streakScale.value
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak produktif",
                            tint = atmo.onAccentAmber,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Streak ${summary.streakDays} Hari",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = atmo.onAccentAmber,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (summary.urgentOrCriticalCount > 0) {
                val urgentScale = remember { Animatable(1f) }
                LaunchedEffect(summary.urgentOrCriticalCount) {
                    urgentScale.snapTo(1.12f)
                    urgentScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                Surface(
                    color = atmo.accentCoral,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = urgentScale.value
                        scaleY = urgentScale.value
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Prioritas mendesak",
                            tint = atmo.onAccentCoral,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "${summary.urgentOrCriticalCount} Mendesak",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = atmo.onAccentCoral,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    val heroProgressRing: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(spec.summaryRingSize),
            contentAlignment = Alignment.Center,
        ) {
            val gaugeColor by animateColorAsState(
                targetValue = if (rawFraction >= 0.999f) atmo.accentMint else atmo.accentAmber,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "hero_gauge_color",
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = (if (spec.isUltraCompact) 5.5.dp else 7.dp).toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.24f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = gaugeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animatedPercent%",
                    style = MonoSmallStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (spec.isUltraCompact) 11.sp else MonoSmallStyle.fontSize,
                    ),
                    color = Color.White,
                )
                Text(
                    text = "${summary.todayCompletedCount}/${summary.todayTotalCount}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (spec.isUltraCompact) 9.5.sp else MaterialTheme.typography.labelSmall.fontSize,
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bouncyPress(cardInteraction, pressedScale = 0.98f)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = atmo.heroSolidBg.copy(alpha = 0.25f),
                spotColor = atmo.heroSolidBg.copy(alpha = 0.30f),
            )
            .clip(shape)
            .clickable(
                enabled = !isSimpleMode,
                interactionSource = cardInteraction,
                indication = null,
            ) {
                haptics.tapSoft()
                onCardClick()
            }
            .testTag("quick_glance_card"),
        shape = shape,
        color = atmo.heroSolidBg,
        contentColor = atmo.heroOnPrimary,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spec.cardPadding),
            verticalArrangement = Arrangement.spacedBy(spec.cardInnerSpacing),
        ) {
            if (spec.isUltraCompact) {
                heroTextBlock()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        heroChipsBlock()
                    }
                    heroProgressRing()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        heroTextBlock()
                        heroChipsBlock()
                    }
                    heroProgressRing()
                }
            }

            if (!isSimpleMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ketuk untuk buka ${tapTarget.labelId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Mode Simpel • Ringkas & Nyaman",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }
            }
        }
    }
}
