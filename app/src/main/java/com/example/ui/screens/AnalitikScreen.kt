package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.DailyChartPoint
import com.example.domain.PriorityQuadrant
import com.example.domain.ScheduleScope
import com.example.ui.JadwalNihUiState
import com.example.ui.components.FrostedGlassCard
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
    val periodOptions = listOf(ScheduleScope.WEEKLY, ScheduleScope.MONTHLY)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "analytics_header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Analitik",
                    style = MaterialTheme.typography.titleLarge,
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
                        SegmentedButton(
                            selected = state.analyticsScope == scope,
                            onClick = { onSelectScope(scope) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = periodOptions.size,
                            ),
                        ) {
                            Text(scope.label)
                        }
                    }
                }
            }
        }

        item(key = "kpi_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiCard(
                        label = "Tuntas",
                        value = "${analytics.completedTasks}/${analytics.totalTasks}",
                        accentBrush = Brush.horizontalGradient(listOf(atmo.accentBlue, atmo.accentPurple)),
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = "Rasio",
                        value = "${analytics.completionRatePercent}%",
                        accentBrush = Brush.horizontalGradient(listOf(atmo.accentPurple, atmo.accentBlue)),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiCard(
                        label = "Menit Fokus",
                        value = "${analytics.totalFocusMinutes} mnt",
                        accentBrush = Brush.horizontalGradient(listOf(palette.blueAccent, atmo.accentPurple)),
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = "Terlewat",
                        value = "${analytics.overdueTasks}",
                        accentBrush = Brush.horizontalGradient(listOf(palette.redAccent, palette.yellowAccent)),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "completion_bar_chart") {
            FrostedGlassCard(cornerRadius = 20.dp) {
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
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LegendDot(color = atmo.accentBlue, label = "Selesai")
                            LegendDot(color = MaterialTheme.colorScheme.outline, label = "Total")
                        }
                    }

                    GradientCompletionBarChart(
                        points = analytics.trendPoints,
                        barStart = atmo.heroGradStart,
                        barEnd = atmo.heroGradEnd,
                        totalColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                    )
                }
            }
        }

        item(key = "quadrant_distribution") {
            FrostedGlassCard(cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Sebaran Kuadran",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    val maxQ = (analytics.quadrantCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    PriorityQuadrant.entries.forEach { q ->
                        val count = analytics.quadrantCounts[q] ?: 0
                        val barBrush = when (q) {
                            PriorityQuadrant.Q1_IMPORTANT_URGENT -> Brush.horizontalGradient(
                                listOf(palette.redAccent, atmo.accentPurple),
                            )
                            PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> Brush.horizontalGradient(
                                listOf(atmo.accentBlue, atmo.accentPurple),
                            )
                            PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> Brush.horizontalGradient(
                                listOf(palette.yellowAccent, atmo.accentBlue),
                            )
                            PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> Brush.horizontalGradient(
                                listOf(palette.grayAccent, atmo.glassBorderEnd),
                            )
                        }
                        val fraction = (count.toFloat() / maxQ.toFloat()).coerceIn(0f, 1f)

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${q.shortCode} • ${q.titleId}",
                                    style = MaterialTheme.typography.bodySmall,
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
                                if (fraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(barBrush),
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

@Composable
private fun KpiCard(
    label: String,
    value: String,
    accentBrush: Brush,
    modifier: Modifier = Modifier,
) {
    FrostedGlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
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
                    .background(accentBrush),
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
private fun GradientCompletionBarChart(
    points: List<DailyChartPoint>,
    barStart: Color,
    barEnd: Color,
    totalColor: Color,
) {
    if (points.isEmpty()) return
    val maxVal = (points.maxOfOrNull { maxOf(it.totalCount, it.completedCount) } ?: 1).coerceAtLeast(1)

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
                val totalH = (pt.totalCount.toFloat() / maxVal.toFloat()) * maxHeight
                val doneH = (pt.completedCount.toFloat() / maxVal.toFloat()) * maxHeight

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
                        brush = Brush.verticalGradient(
                            colors = listOf(barEnd, barStart),
                            startY = topY,
                            endY = size.height,
                        ),
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
