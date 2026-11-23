package com.example.widget

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.R
import com.example.domain.DateTimeHelper
import com.example.domain.TaskIconCatalog
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.domain.WidgetPrioritySummary
import com.example.domain.WidgetQuadrantFilter
import com.example.domain.WorshipScheduleItem

enum class WidgetSizeTier {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

fun resolveWidgetSizeTier(size: DpSize): WidgetSizeTier = when {
    size.width < 220.dp || size.height < 145.dp -> WidgetSizeTier.COMPACT
    size.height < 230.dp -> WidgetSizeTier.MEDIUM
    else -> WidgetSizeTier.EXPANDED
}

private fun GlanceModifier.widgetOuterCorners(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        this.cornerRadius(22.dp)
    }

@Composable
fun PriorityQuadrantWidgetContent(
    summary: WidgetPrioritySummary,
    onCycleFilter: Action,
    onSelectFilter: (WidgetQuadrantFilter) -> Action,
    onToggleTask: (Long) -> Action,
    onQuickAdd: Action,
    onOpenApp: Action,
    onToggleWorship: (WorshipScheduleItem) -> Action = { onOpenApp },
) {
    val size = LocalSize.current
    val tier = resolveWidgetSizeTier(size)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(PriorityQuadrantWidgetColors.widgetBackground)
            .widgetOuterCorners()
            .padding(
                when (tier) {
                    WidgetSizeTier.COMPACT -> 10.dp
                    WidgetSizeTier.MEDIUM -> 12.dp
                    WidgetSizeTier.EXPANDED -> 14.dp
                },
            )
            .semantics { testTag = "widget_root" },
    ) {
        when (tier) {
            WidgetSizeTier.COMPACT -> CompactWidgetBody(
                summary = summary,
                onCycleFilter = onCycleFilter,
                onToggleTask = onToggleTask,
                onQuickAdd = onQuickAdd,
                onOpenApp = onOpenApp,
            )
            WidgetSizeTier.MEDIUM -> MediumWidgetBody(
                summary = summary,
                onCycleFilter = onCycleFilter,
                onToggleTask = onToggleTask,
                onQuickAdd = onQuickAdd,
                onOpenApp = onOpenApp,
                onToggleWorship = onToggleWorship,
            )
            WidgetSizeTier.EXPANDED -> ExpandedWidgetBody(
                summary = summary,
                onSelectFilter = onSelectFilter,
                onToggleTask = onToggleTask,
                onQuickAdd = onQuickAdd,
                onOpenApp = onOpenApp,
                onToggleWorship = onToggleWorship,
            )
        }
    }
}

@Composable
private fun CompactWidgetBody(
    summary: WidgetPrioritySummary,
    onCycleFilter: Action,
    onToggleTask: (Long) -> Action,
    onQuickAdd: Action,
    onOpenApp: Action,
) {
    val visibleTasks = summary.activeTasks.take(2)
    val extraCount = (summary.activeTasks.size - visibleTasks.size).coerceAtLeast(0)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .semantics { testTag = "widget_tier_compact" },
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(PriorityQuadrantWidgetColors.filterSelectedBg)
                    .cornerRadius(12.dp)
                    .clickable(onCycleFilter)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .semantics { testTag = "widget_filter_cycle_button" },
            ) {
                Text(
                    text = summary.filter.shortLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = PriorityQuadrantWidgetColors.filterSelectedFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .size(30.dp)
                    .background(PriorityQuadrantWidgetColors.accentCobalt)
                    .cornerRadius(12.dp)
                    .clickable(onQuickAdd)
                    .semantics { testTag = "widget_quick_add_button" },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "Tambah tugas cepat",
                    colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.onAccentCobalt),
                    modifier = GlanceModifier.size(18.dp),
                )
            }
        }

        Spacer(GlanceModifier.height(6.dp))

        if (visibleTasks.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(PriorityQuadrantWidgetColors.cardSurface)
                    .cornerRadius(14.dp)
                    .clickable(onOpenApp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_check_done),
                        contentDescription = "Selesai semua",
                        colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.accentMint),
                        modifier = GlanceModifier.size(20.dp),
                    )
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        text = "Bersih • Ketuk +",
                        maxLines = 1,
                        style = TextStyle(
                            color = PriorityQuadrantWidgetColors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                visibleTasks.forEachIndexed { idx, item ->
                    if (idx > 0) Spacer(GlanceModifier.height(4.dp))
                    CompactTaskRow(
                        item = item,
                        onToggleTask = onToggleTask,
                        onOpenApp = onOpenApp,
                    )
                }
                if (extraCount > 0) {
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        text = "+$extraCount tugas lagi",
                        maxLines = 1,
                        modifier = GlanceModifier
                            .clickable(onOpenApp)
                            .padding(horizontal = 4.dp),
                        style = TextStyle(
                            color = PriorityQuadrantWidgetColors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTaskRow(
    item: TaskWithStatus,
    onToggleTask: (Long) -> Action,
    onOpenApp: Action,
) {
    val task = item.task
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(PriorityQuadrantWidgetColors.cardSurface)
            .cornerRadius(12.dp)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(24.dp)
                .clickable(onToggleTask(task.id))
                .semantics { testTag = "widget_task_check_${task.id}" },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_check_outline),
                contentDescription = "Centang selesai ${task.title}",
                colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.quadrantAccent(task.quadrant)),
                modifier = GlanceModifier.size(16.dp),
            )
        }
        Spacer(GlanceModifier.width(4.dp))
        Image(
            provider = ImageProvider(TaskIconCatalog.widgetDrawableRes(task.iconKey, task.title)),
            contentDescription = null,
            colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.textSecondary),
            modifier = GlanceModifier.size(13.dp),
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = task.title,
            maxLines = 1,
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(onOpenApp),
            style = TextStyle(
                color = PriorityQuadrantWidgetColors.textOnCard,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun MediumWidgetBody(
    summary: WidgetPrioritySummary,
    onCycleFilter: Action,
    onToggleTask: (Long) -> Action,
    onQuickAdd: Action,
    onOpenApp: Action,
    onToggleWorship: (WorshipScheduleItem) -> Action,
) {
    val worshipItem = if (summary.worshipEnabled) summary.nextWorship else null
    val visibleTasks = summary.activeTasks.take(if (worshipItem != null) 2 else 3)
    val extraCount = (summary.activeTasks.size - visibleTasks.size).coerceAtLeast(0)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .semantics { testTag = "widget_tier_medium" },
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier
                    .background(PriorityQuadrantWidgetColors.filterSelectedBg)
                    .cornerRadius(14.dp)
                    .clickable(onCycleFilter)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { testTag = "widget_filter_cycle_button" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summary.filter.shortLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = PriorityQuadrantWidgetColors.filterSelectedFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.width(4.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_swap),
                    contentDescription = "Ganti kuadran",
                    colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.filterSelectedFg),
                    modifier = GlanceModifier.size(14.dp),
                )
            }

            Spacer(GlanceModifier.width(8.dp))

            Text(
                text = if (summary.activeTasks.isEmpty()) {
                    "Tuntas"
                } else {
                    "${summary.activeTasks.size} aktif"
                },
                maxLines = 1,
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(onOpenApp),
                style = TextStyle(
                    color = PriorityQuadrantWidgetColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Row(
                modifier = GlanceModifier
                    .background(PriorityQuadrantWidgetColors.accentCobalt)
                    .cornerRadius(14.dp)
                    .clickable(onQuickAdd)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { testTag = "widget_quick_add_button" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "Tambah cepat",
                    colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.onAccentCobalt),
                    modifier = GlanceModifier.size(15.dp),
                )
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    text = "Quick-Add",
                    maxLines = 1,
                    style = TextStyle(
                        color = PriorityQuadrantWidgetColors.onAccentCobalt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        if (worshipItem != null) {
            Spacer(GlanceModifier.height(6.dp))
            WidgetWorshipDualBar(
                item = worshipItem,
                countdownLabel = summary.worshipCountDownLabel,
                onToggleWorship = onToggleWorship(worshipItem),
                onOpenApp = onOpenApp,
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        if (visibleTasks.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(PriorityQuadrantWidgetColors.cardSurface)
                    .cornerRadius(16.dp)
                    .clickable(onQuickAdd)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_check_done),
                        contentDescription = "Semua beres",
                        colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.accentMint),
                        modifier = GlanceModifier.size(22.dp),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = "Semua tugas di ${summary.filter.shortLabel} aman! Ketuk untuk tambah.",
                        maxLines = 2,
                        style = TextStyle(
                            color = PriorityQuadrantWidgetColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                visibleTasks.forEachIndexed { idx, item ->
                    if (idx > 0) Spacer(GlanceModifier.height(5.dp))
                    MediumTaskRow(
                        item = item,
                        onToggleTask = onToggleTask,
                        onOpenApp = onOpenApp,
                    )
                }
                if (extraCount > 0) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = "+$extraCount jadwal lagi • Perbesar widget untuk lihat semua",
                        maxLines = 1,
                        modifier = GlanceModifier
                            .clickable(onOpenApp)
                            .padding(horizontal = 4.dp),
                        style = TextStyle(
                            color = PriorityQuadrantWidgetColors.textSecondary,
                            fontSize = 10.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediumTaskRow(
    item: TaskWithStatus,
    onToggleTask: (Long) -> Action,
    onOpenApp: Action,
) {
    val task = item.task
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(PriorityQuadrantWidgetColors.cardSurface)
            .cornerRadius(14.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .clickable(onToggleTask(task.id))
                .semantics { testTag = "widget_task_check_${task.id}" },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_check_outline),
                contentDescription = "Centang selesai ${task.title}",
                colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.quadrantAccent(task.quadrant)),
                modifier = GlanceModifier.size(18.dp),
            )
        }
        Spacer(GlanceModifier.width(4.dp))
        Box(
            modifier = GlanceModifier
                .background(PriorityQuadrantWidgetColors.quadrantBg(task.quadrant))
                .cornerRadius(8.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = task.quadrant.shortCode,
                style = TextStyle(
                    color = PriorityQuadrantWidgetColors.quadrantFg(task.quadrant),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(TaskIconCatalog.widgetDrawableRes(task.iconKey, task.title)),
            contentDescription = null,
            colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.accentCobalt),
            modifier = GlanceModifier.size(14.dp),
        )
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = task.title,
            maxLines = 1,
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(onOpenApp),
            style = TextStyle(
                color = PriorityQuadrantWidgetColors.textOnCard,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = DateTimeHelper.formatMinuteOfDay(task.startMinuteOfDay),
            maxLines = 1,
            style = TextStyle(
                color = PriorityQuadrantWidgetColors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun ExpandedWidgetBody(
    summary: WidgetPrioritySummary,
    onSelectFilter: (WidgetQuadrantFilter) -> Action,
    onToggleTask: (Long) -> Action,
    onQuickAdd: Action,
    onOpenApp: Action,
    onToggleWorship: (WorshipScheduleItem) -> Action,
) {
    val worshipItem = if (summary.worshipEnabled) summary.nextWorship else null
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .semantics { testTag = "widget_tier_expanded" },
    ) {
        // Top Header: Brand, Date, Quick-Add CTA, Open App
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(onOpenApp),
            ) {
                Text(
                    text = "Prioritas Kuadran",
                    maxLines = 1,
                    style = TextStyle(
                        color = PriorityQuadrantWidgetColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "${DateTimeHelper.formatShortDate(summary.todayDateIso)} • ${summary.totalTodayCompletedCount}/${summary.totalTodayCount} selesai hari ini",
                    maxLines = 1,
                    style = TextStyle(
                        color = PriorityQuadrantWidgetColors.textSecondary,
                        fontSize = 11.sp,
                    ),
                )
            }

            Row(
                modifier = GlanceModifier
                    .background(PriorityQuadrantWidgetColors.accentCobalt)
                    .cornerRadius(14.dp)
                    .clickable(onQuickAdd)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .semantics { testTag = "widget_quick_add_button" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "Quick-Add tugas",
                    colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.onAccentCobalt),
                    modifier = GlanceModifier.size(16.dp),
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "Quick-Add",
                    maxLines = 1,
                    style = TextStyle(
                        color = PriorityQuadrantWidgetColors.onAccentCobalt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(GlanceModifier.width(6.dp))

            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(PriorityQuadrantWidgetColors.cardAltSurface)
                    .cornerRadius(12.dp)
                    .clickable(onOpenApp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_open_app),
                    contentDescription = "Buka JadwalNih",
                    colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.textSecondary),
                    modifier = GlanceModifier.size(16.dp),
                )
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        // Interactive Quadrant Filter Chips
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetQuadrantFilter.entries.forEachIndexed { idx, filterOption ->
                if (idx > 0) Spacer(GlanceModifier.width(4.dp))
                val isSelected = summary.filter == filterOption
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(
                            if (isSelected) {
                                PriorityQuadrantWidgetColors.filterSelectedBg
                            } else {
                                PriorityQuadrantWidgetColors.cardAltSurface
                            },
                        )
                        .cornerRadius(12.dp)
                        .clickable(onSelectFilter(filterOption))
                        .padding(vertical = 5.dp)
                        .semantics { testTag = "widget_filter_chip_${filterOption.name}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = filterOption.chipLabel,
                        maxLines = 1,
                        style = TextStyle(
                            color = if (isSelected) {
                                PriorityQuadrantWidgetColors.filterSelectedFg
                            } else {
                                PriorityQuadrantWidgetColors.textSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        if (worshipItem != null) {
            Spacer(GlanceModifier.height(6.dp))
            WidgetWorshipDualBar(
                item = worshipItem,
                countdownLabel = summary.worshipCountDownLabel,
                onToggleWorship = onToggleWorship(worshipItem),
                onOpenApp = onOpenApp,
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        if (summary.activeTasks.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(PriorityQuadrantWidgetColors.cardSurface)
                    .cornerRadius(18.dp)
                    .clickable(onQuickAdd)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_check_done),
                        contentDescription = "Kosong",
                        colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.accentMint),
                        modifier = GlanceModifier.size(28.dp),
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text = "Tidak ada tugas aktif di ${summary.filter.fullLabel}",
                        maxLines = 2,
                        style = TextStyle(
                            color = PriorityQuadrantWidgetColors.textOnCard,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        text = "Ketuk untuk Quick-Add ke kuadran ini",
                        maxLines = 1,
                        style = TextStyle(
                            color = PriorityQuadrantWidgetColors.accentCobalt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(
                    items = summary.activeTasks.take(12),
                    itemId = { it.task.id },
                ) { item ->
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        ExpandedTaskCard(
                            item = item,
                            onToggleTask = onToggleTask,
                            onOpenApp = onOpenApp,
                        )
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedTaskCard(
    item: TaskWithStatus,
    onToggleTask: (Long) -> Action,
    onOpenApp: Action,
) {
    val task = item.task
    val durationText = DateTimeHelper.formatDuration(
        (task.endMinuteOfDay - task.startMinuteOfDay).coerceAtLeast(1),
    )
    val subDoneCount = task.subtasks.count { it.isDone }
    val showUrgentStatus = item.status == TaskStatus.CRITICAL_RED ||
        item.status == TaskStatus.APPROACHING_YELLOW ||
        item.status == TaskStatus.OVERDUE_DARK

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(PriorityQuadrantWidgetColors.cardSurface)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .clickable(onToggleTask(task.id))
                .semantics { testTag = "widget_task_check_${task.id}" },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_check_outline),
                contentDescription = "Centang selesai ${task.title}",
                colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.quadrantAccent(task.quadrant)),
                modifier = GlanceModifier.size(20.dp),
            )
        }

        Spacer(GlanceModifier.width(6.dp))

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(onOpenApp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .background(PriorityQuadrantWidgetColors.quadrantBg(task.quadrant))
                        .cornerRadius(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${task.quadrant.shortCode} • ${task.quadrant.chipLabelId}",
                        style = TextStyle(
                            color = PriorityQuadrantWidgetColors.quadrantFg(task.quadrant),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                if (showUrgentStatus) {
                    Spacer(GlanceModifier.width(4.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(PriorityQuadrantWidgetColors.statusBg(item.status))
                            .cornerRadius(8.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = item.status.labelId,
                            style = TextStyle(
                                color = PriorityQuadrantWidgetColors.statusFg(item.status),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }

            Spacer(GlanceModifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(TaskIconCatalog.widgetDrawableRes(task.iconKey, task.title)),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.accentCobalt),
                    modifier = GlanceModifier.size(15.dp),
                )
                Spacer(GlanceModifier.width(5.dp))
                Text(
                    text = task.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = PriorityQuadrantWidgetColors.textOnCard,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(GlanceModifier.height(2.dp))

            val metaParts = buildList {
                add(DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay))
                add(durationText)
                if (task.subtasks.isNotEmpty()) {
                    add("$subDoneCount/${task.subtasks.size} langkah")
                }
                if (task.locationName.isNotBlank()) {
                    add(task.locationName)
                }
            }.joinToString(" • ")

            Text(
                text = metaParts,
                maxLines = 1,
                style = TextStyle(
                    color = PriorityQuadrantWidgetColors.textSecondary,
                    fontSize = 11.sp,
                ),
            )
        }
    }
}

@Composable
private fun WidgetWorshipDualBar(
    item: WorshipScheduleItem,
    countdownLabel: String,
    onToggleWorship: Action,
    onOpenApp: Action,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(PriorityQuadrantWidgetColors.cardAltSurface)
            .cornerRadius(12.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .semantics { testTag = "widget_worship_bar" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(26.dp)
                .clickable(onToggleWorship)
                .semantics { testTag = "widget_worship_toggle_button" },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(
                    if (item.isCompleted) R.drawable.ic_widget_check_done else R.drawable.ic_widget_check_outline,
                ),
                contentDescription = "Tandai ${item.title} selesai",
                colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.accentMint),
                modifier = GlanceModifier.size(17.dp),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(onOpenApp)
                .semantics { testTag = "widget_worship_open_app" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_cat_worship),
                contentDescription = null,
                colorFilter = ColorFilter.tint(PriorityQuadrantWidgetColors.accentMint),
                modifier = GlanceModifier.size(14.dp),
            )
            Spacer(GlanceModifier.width(5.dp))
            Text(
                text = buildString {
                    append("${item.title} ${item.formattedTime}")
                    if (countdownLabel.isNotBlank()) append(" • $countdownLabel")
                },
                maxLines = 1,
                style = TextStyle(
                    color = PriorityQuadrantWidgetColors.textOnCard,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
