package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.DailyChartPoint
import com.example.domain.HeatmapDayCell
import com.example.domain.PriorityQuadrant
import com.example.domain.ScheduleScope
import com.example.ui.JadwalNihUiState
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.WeeklyWorshipAnalyticsCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.tapSoft
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.MonoStatStyle

@Composable
fun AnalitikScreen(
    state: JadwalNihUiState,
    onSelectScope: (ScheduleScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    val analytics = state.analytics
    val palette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val haptics = LocalHapticFeedback.current
    val periodOptions = listOf(ScheduleScope.WEEKLY, ScheduleScope.MONTHLY)

    val animCompleted by animateIntAsState(
        targetValue = analytics.completedTasks,
        animationSpec = tween(550),
        label = "kpi_completed",
    )
    val animTotal by animateIntAsState(
        targetValue = analytics.totalTasks,
        animationSpec = tween(550),
        label = "kpi_total",
    )
    val animRate by animateIntAsState(
        targetValue = analytics.completionRatePercent,
        animationSpec = tween(600),
        label = "kpi_rate",
    )
    val animFocus by animateIntAsState(
        targetValue = analytics.totalFocusMinutes,
        animationSpec = tween(650),
        label = "kpi_focus",
    )
    val animOverdue by animateIntAsState(
        targetValue = analytics.overdueTasks,
        animationSpec = tween(550),
        label = "kpi_overdue",
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spec.screenHorizontalPadding,
            end = spec.screenHorizontalPadding,
            top = 8.dp,
            bottom = spec.bottomNavClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "analytics_header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Analitik",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Pantau capaian tugas & waktu fokusmu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_scope_segmented"),
                ) {
                    periodOptions.forEachIndexed { idx, scope ->
                        val segInteraction = remember { MutableInteractionSource() }
                        SegmentedButton(
                            selected = state.analyticsScope == scope,
                            onClick = {
                                haptics.tapSoft()
                                onSelectScope(scope)
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = periodOptions.size,
                            ),
                            interactionSource = segInteraction,
                            modifier = Modifier.bouncyPress(segInteraction, pressedScale = 0.93f),
                        ) {
                            Text(scope.label)
                        }
                    }
                }
            }
        }

        item(key = "kpi_grid") {
            StaggeredEntrance(index = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KpiCard(
                            label = "Tuntas",
                            value = "$animCompleted/$animTotal",
                            accentColor = atmo.accentMint,
                            modifier = Modifier.weight(1f),
                        )
                        KpiCard(
                            label = "Rasio",
                            value = "$animRate%",
                            accentColor = atmo.accentCobalt,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KpiCard(
                            label = "Menit Fokus",
                            value = "$animFocus mnt",
                            accentColor = atmo.accentOrange,
                            modifier = Modifier.weight(1f),
                        )
                        KpiCard(
                            label = "Terlewat",
                            value = "$animOverdue",
                            accentColor = atmo.accentCoral,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item(key = "worship_analytics") {
            StaggeredEntrance(index = 1) {
                WeeklyWorshipAnalyticsCard(summary = analytics.worshipSummary)
            }
        }

        item(key = "consistency_heatmap") {
            StaggeredEntrance(index = 2) {
                ConsistencyHeatmapCard(
                    streakDays = analytics.streakDays,
                    cells = analytics.heatmapDays,
                )
            }
        }

        item(key = "completion_bar_chart") {
            StaggeredEntrance(index = 2) {
                FrostedGlassCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Grafik Tugas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot(color = atmo.accentCobalt, label = "Selesai")
                                LegendDot(color = MaterialTheme.colorScheme.outline, label = "Total")
                            }
                        }

                        SolidCompletionBarChart(
                            points = analytics.trendPoints,
                            barColor = atmo.accentCobalt,
                            totalColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                        )
                    }
                }
            }
        }

        item(key = "quadrant_distribution") {
            StaggeredEntrance(index = 3) {
                FrostedGlassCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    Text(
                        text = "Sebaran Kuadran",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    val maxQ = (analytics.quadrantCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    PriorityQuadrant.entries.forEach { q ->
                        val count = analytics.quadrantCounts[q] ?: 0
                        val barColor = when (q) {
                            PriorityQuadrant.Q1_IMPORTANT_URGENT -> palette.redAccent
                            PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> palette.blueAccent
                            PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> palette.yellowAccent
                            PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> palette.greenAccent
                        }
                        val targetFraction = (count.toFloat() / maxQ.toFloat()).coerceIn(0f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = targetFraction,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "quadrant_bar_${q.name}",
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${q.shortCode} • ${q.titleId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "$count tugas",
                                    style = MonoSmallStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                            ) {
                                if (animatedFraction > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedFraction)
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(barColor),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ConsistencyHeatmapCard(
    streakDays: Int,
    cells: List<HeatmapDayCell>,
) {
    val atmo = LocalAtmospherePalette.current
    val dayHeaders = listOf("S", "S", "R", "K", "J", "S", "M")

    FrostedGlassCard(
        modifier = Modifier.testTag("consistency_heatmap_card"),
        cornerRadius = 24.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Konsistensi 5 Minggu",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Aktivitas tuntas & fokus 35 hari",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = atmo.accentAmber,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak produktif",
                            tint = atmo.onAccentAmber,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Streak $streakDays Hari",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = atmo.onAccentAmber,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dayHeaders.forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { weekRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        weekRow.forEach { cell ->
                            val cellColor by animateColorAsState(
                                targetValue = heatmapCellColor(cell, atmo),
                                animationSpec = tween(durationMillis = 260),
                                label = "heatmap_cell_${cell.dateIso}",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.45f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(cellColor),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sepi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                (0..4).forEach { lvl ->
                    val sampleCell = HeatmapDayCell("", 0, 0, lvl, isFuture = false)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(heatmapCellColor(sampleCell, atmo)),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = "Ramai",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun heatmapCellColor(
    cell: HeatmapDayCell,
    atmo: com.example.ui.theme.AtmospherePalette,
): Color {
    if (cell.isFuture) {
        return atmo.surfaceSubtle
    }
    return when (cell.level) {
        0 -> atmo.surfaceMuted
        1 -> atmo.accentMint.copy(alpha = if (atmo.isDark) 0.34f else 0.28f)
        2 -> atmo.accentMint.copy(alpha = if (atmo.isDark) 0.56f else 0.50f)
        3 -> atmo.accentMint.copy(alpha = if (atmo.isDark) 0.78f else 0.74f)
        else -> atmo.accentMint
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    FrostedGlassCard(
        modifier = modifier,
        cornerRadius = 22.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MonoStatStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SolidCompletionBarChart(
    points: List<DailyChartPoint>,
    barColor: Color,
    totalColor: Color,
) {
    if (points.isEmpty()) return
    val maxVal = (points.maxOfOrNull { maxOf(it.totalCount, it.completedCount) } ?: 1).coerceAtLeast(1)

    var targetReveal by remember(points) { mutableFloatStateOf(0f) }
    LaunchedEffect(points) {
        targetReveal = 1f
    }
    val animReveal by animateFloatAsState(
        targetValue = targetReveal,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "bar_chart_reveal",
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp),
        ) {
            val count = points.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.28f).coerceAtMost(24.dp.toPx())
            val maxHeight = size.height - 12.dp.toPx()

            points.forEachIndexed { idx, pt ->
                val centerX = slotWidth * idx + slotWidth / 2f
                val totalH = (pt.totalCount.toFloat() / maxVal.toFloat()) * maxHeight * animReveal
                val doneH = (pt.completedCount.toFloat() / maxVal.toFloat()) * maxHeight * animReveal

                if (pt.totalCount > 0) {
                    drawRoundRect(
                        color = totalColor,
                        topLeft = Offset(centerX - barWidth - 2.dp.toPx(), size.height - totalH),
                        size = Size(barWidth, totalH.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    )
                }

                if (pt.completedCount > 0) {
                    val topY = size.height - doneH
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX + 2.dp.toPx(), topY),
                        size = Size(barWidth, doneH.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { pt ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${pt.completedCount}/${pt.totalCount}",
                        style = MonoSmallStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

