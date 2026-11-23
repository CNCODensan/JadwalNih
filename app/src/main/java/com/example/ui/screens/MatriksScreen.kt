package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.DailyTimelineEntry
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoRecap
import com.example.domain.PriorityQuadrant
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.TaskWithStatus
import com.example.ui.JadwalNihUiState
import com.example.ui.components.BouncyFilterChip
import com.example.ui.components.BouncyIconButton
import com.example.ui.components.BouncyOutlinedButton
import com.example.ui.components.BouncyTextButton
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.QuadrantChip
import com.example.ui.components.StatusBadge
import com.example.ui.components.bouncyPress
import com.example.ui.components.colorsFor
import com.example.ui.components.tapConfirm
import com.example.ui.components.tapSoft
import com.example.ui.components.tapTick
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle
import java.io.File

@Composable
fun MatriksScreen(
    state: JadwalNihUiState,
    onSelectScope: (ScheduleScope) -> Unit,
    onSelectQuadrantFilter: (PriorityQuadrant?) -> Unit,
    onStepDate: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectSharedDate: (String) -> Unit,
    onAddTaskAtHour: (String, Int) -> Unit,
    onOpenRecapForDate: (String) -> Unit,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayIso = remember { DateTimeHelper.todayIso() }
    val statusPalette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current

    val hourlyEntries = remember(state.matrixTasks, state.matrixScope) {
        if (state.matrixScope == ScheduleScope.DAILY) {
            ScheduleAnalyzer.buildHourlyTimeline(state.matrixTasks)
        } else {
            emptyList()
        }
    }

    val weekDays = remember(state.selectedDateIso, state.matrixScope) {
        if (state.matrixScope == ScheduleScope.WEEKLY) {
            DateTimeHelper.weekDaysFor(state.selectedDateIso)
        } else {
            emptyList()
        }
    }

    var monthlyAnchorIso by remember(state.selectedRecapDateIso) {
        mutableStateOf(state.selectedRecapDateIso)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "matrix_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Matriks & Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Pantau timeline per jam, mingguan, dan kalender bulanan dengan filter kuadran.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("matrix_scope_segmented"),
                ) {
                    ScheduleScope.entries.forEachIndexed { idx, scope ->
                        val segInteraction = remember { MutableInteractionSource() }
                        SegmentedButton(
                            selected = state.matrixScope == scope,
                            onClick = {
                                haptics.tapSoft()
                                onSelectScope(scope)
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = ScheduleScope.entries.size,
                            ),
                            interactionSource = segInteraction,
                            modifier = Modifier
                                .bouncyPress(segInteraction, pressedScale = 0.93f)
                                .testTag("matrix_scope_${scope.name.lowercase()}"),
                        ) {
                            Text(scope.label)
                        }
                    }
                }

                // Quadrant Filter Row active across Harian, Mingguan, Bulanan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BouncyFilterChip(
                        selected = state.matrixQuadrantFilter == null,
                        onClick = {
                            haptics.tapSoft()
                            onSelectQuadrantFilter(null)
                        },
                        label = { Text("Semua") },
                        modifier = Modifier.testTag("matrix_quad_filter_all"),
                    )
                    PriorityQuadrant.entries.forEach { q ->
                        val (qBg, qFg, qAccent) = quadrantThemeColors(q)
                        BouncyFilterChip(
                            selected = state.matrixQuadrantFilter == q,
                            onClick = {
                                haptics.tapSoft()
                                onSelectQuadrantFilter(q)
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(qAccent),
                                )
                            },
                            label = { Text("${q.shortCode} • ${q.chipLabelId}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = qBg,
                                selectedLabelColor = qFg,
                            ),
                            modifier = Modifier.testTag("matrix_quad_filter_${q.shortCode}"),
                        )
                    }
                }
            }
        }

        when (state.matrixScope) {
            ScheduleScope.DAILY -> {
                item(key = "daily_timeline_bar") {
                    FrostedGlassCard(cornerRadius = 16.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                BouncyIconButton(
                                    onClick = {
                                        haptics.tapTick()
                                        onStepDate(-1)
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Hari sebelumnya",
                                    )
                                }
                                Column {
                                    Text(
                                        text = DateTimeHelper.formatIndonesianFullDate(state.selectedDateIso),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (state.selectedDateIso != todayIso) {
                                        Text(
                                            text = "Ketuk untuk ke Hari Ini",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = atmo.accentCobalt,
                                            modifier = Modifier.clickable {
                                                haptics.tapSoft()
                                                onJumpToToday()
                                            },
                                        )
                                    }
                                }
                                BouncyIconButton(
                                    onClick = {
                                        haptics.tapTick()
                                        onStepDate(1)
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Hari berikutnya",
                                    )
                                }
                            }

                            BouncyTextButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onAddTaskAtHour(state.selectedDateIso, 9)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("matrix_daily_add_button"),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tambah", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(
                    items = hourlyEntries,
                    key = { entry ->
                        when (entry) {
                            is DailyTimelineEntry.HourSlotEmpty -> "empty_${entry.hour}"
                            is DailyTimelineEntry.TaskBlock -> "task_${entry.item.task.id}"
                            is DailyTimelineEntry.GapBreak -> "gap_${entry.startMinuteOfDay}_${entry.endMinuteOfDay}"
                        }
                    },
                ) { entry ->
                    when (entry) {
                        is DailyTimelineEntry.HourSlotEmpty -> {
                            EmptyHourSlotRow(
                                entry = entry,
                                onAddAtHour = {
                                    haptics.tapSoft()
                                    onAddTaskAtHour(state.selectedDateIso, entry.hour)
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }

                        is DailyTimelineEntry.GapBreak -> {
                            TimelineGapRow(
                                entry = entry,
                                modifier = Modifier.animateItem(),
                            )
                        }

                        is DailyTimelineEntry.TaskBlock -> {
                            TimelineTaskBlockRow(
                                block = entry,
                                onToggleComplete = onToggleComplete,
                                onMoveToTomorrow = onMoveToTomorrow,
                                onChangeQuadrant = onChangeQuadrant,
                                onEditTask = onEditTask,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            ScheduleScope.WEEKLY -> {
                item(key = "weekly_hub_header") {
                    FrostedGlassCard(cornerRadius = 16.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            BouncyIconButton(
                                onClick = {
                                    haptics.tapTick()
                                    onStepDate(-7)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Minggu sebelumnya")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Pusat Jadwal Mingguan",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${DateTimeHelper.formatShortDate(weekDays.first())} – ${DateTimeHelper.formatShortDate(weekDays.last())}",
                                    style = MonoSmallStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BouncyIconButton(
                                onClick = {
                                    haptics.tapTick()
                                    onStepDate(7)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Minggu berikutnya")
                            }
                        }
                    }
                }

                items(
                    items = weekDays,
                    key = { "week_day_$it" },
                ) { dayIso ->
                    val dayTasks = remember(state.matrixTasks, dayIso) {
                        state.matrixTasks
                            .filter { it.task.dateIso == dayIso }
                            .sortedBy { it.task.startMinuteOfDay }
                    }
                    WeeklyDayHubCard(
                        dayIso = dayIso,
                        isToday = dayIso == todayIso,
                        tasks = dayTasks,
                        onAddForDay = {
                            haptics.tapSoft()
                            onAddTaskAtHour(dayIso, 9)
                        },
                        onToggleComplete = onToggleComplete,
                        onMoveToTomorrow = onMoveToTomorrow,
                        onChangeQuadrant = onChangeQuadrant,
                        onEditTask = onEditTask,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            ScheduleScope.MONTHLY -> {
                item(key = "monthly_synced_calendar") {
                    SyncedMonthlyCalendarCard(
                        visibleMonthAnchor = monthlyAnchorIso,
                        selectedDateIso = state.selectedRecapDateIso,
                        todayIso = todayIso,
                        tasks = state.matrixTasks,
                        photoRecaps = state.photoRecaps,
                        onChangeMonth = { delta ->
                            monthlyAnchorIso = DateTimeHelper.addMonths(monthlyAnchorIso, delta)
                        },
                        onSelectDate = { chosen ->
                            monthlyAnchorIso = chosen
                            onSelectSharedDate(chosen)
                        },
                        onJumpToday = {
                            monthlyAnchorIso = todayIso
                            onSelectSharedDate(todayIso)
                        },
                    )
                }

                item(key = "monthly_selected_day_detail") {
                    AnimatedContent(
                        targetState = state.selectedRecapDateIso,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInVertically { it / 12 })
                                .togetherWith(fadeOut(tween(150)))
                        },
                        label = "monthly_day_detail_transition",
                    ) { selectedIso ->
                        val dayRecap = state.photoRecaps[selectedIso]
                        val dayTasks = remember(state.matrixTasks, selectedIso) {
                            state.matrixTasks
                                .filter { it.task.dateIso == selectedIso }
                                .sortedBy { it.task.startMinuteOfDay }
                        }
                        MonthlyDayDetailCard(
                            dateIso = selectedIso,
                            recap = dayRecap,
                            tasks = dayTasks,
                            onOpenRecapForDate = { onOpenRecapForDate(selectedIso) },
                            onAddTaskForDate = { onAddTaskAtHour(selectedIso, 9) },
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHourSlotRow(
    entry: DailyTimelineEntry.HourSlotEmpty,
    onAddAtHour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .bouncyPress(interaction, pressedScale = 0.98f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onAddAtHour,
            )
            .padding(vertical = 4.dp)
            .testTag("timeline_empty_hour_${entry.hour}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.label,
            style = MonoSmallStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            )
        }
        Surface(
            color = atmo.cardAltBg.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Kosong • Ketuk untuk tambah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Tambah jadwal pukul ${entry.label}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TimelineGapRow(
    entry: DailyTimelineEntry.GapBreak,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 76.dp),
    ) {
        Surface(
            color = atmo.warmTintBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, atmo.accentOrange.copy(alpha = 0.32f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.FreeBreakfast,
                    contentDescription = null,
                    tint = atmo.accentOrange,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Jeda • ${DateTimeHelper.formatMinuteDot(entry.startMinuteOfDay)}–${
                        DateTimeHelper.formatMinuteDot(entry.endMinuteOfDay)
                    } • ${DateTimeHelper.formatTimelineDuration(entry.durationMinutes)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = atmo.accentOrange,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineTaskBlockRow(
    block: DailyTimelineEntry.TaskBlock,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = block.item
    val task = item.task
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val (_, _, quadrantAccent) = quadrantThemeColors(task.quadrant)
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("timeline_task_block_${task.id}"),
    ) {
        // Left Time Stack (start bold, end muted)
        Column(
            modifier = Modifier
                .width(52.dp)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = DateTimeHelper.formatMinuteDot(task.startMinuteOfDay),
                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = DateTimeHelper.formatMinuteDot(task.endMinuteOfDay),
                style = MonoSmallStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Center Timeline Node + Spine Line
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, quadrantAccent, CircleShape)
                    .background(atmo.bgTop),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .padding(top = 4.dp)
                    .background(quadrantAccent.copy(alpha = 0.28f)),
            )
        }

        // Right Event Card with colored left strip
        val rowInteraction = remember { MutableInteractionSource() }
        Surface(
            onClick = {
                haptics.tapSoft()
                onEditTask(task)
            },
            interactionSource = rowInteraction,
            color = atmo.cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, atmo.cardBorder),
            modifier = Modifier
                .weight(1f)
                .bouncyPress(rowInteraction, pressedScale = 0.97f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(quadrantAccent),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                ),
                                color = if (task.isCompleted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = DateTimeHelper.formatTimelineDuration(block.durationMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = {
                                    if (!task.isCompleted) haptics.tapConfirm() else haptics.tapSoft()
                                    onToggleComplete(task)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = atmo.accentMint),
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                            Box {
                                BouncyIconButton(
                                    onClick = {
                                        haptics.tapSoft()
                                        menuExpanded = true
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Menu jadwal",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Pindah ke Besok") },
                                        leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                                        onClick = {
                                            haptics.tapSoft()
                                            menuExpanded = false
                                            onMoveToTomorrow(task)
                                        },
                                    )
                                    PriorityQuadrant.entries.filter { it != task.quadrant }.forEach { target ->
                                        DropdownMenuItem(
                                            text = { Text("Pindah ke ${target.shortCode}: ${target.titleId}") },
                                            onClick = {
                                                haptics.tapSoft()
                                                menuExpanded = false
                                                onChangeQuadrant(task, target)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatusBadge(status = item.status)
                        QuadrantChip(quadrant = task.quadrant)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyDayHubCard(
    dayIso: String,
    isToday: Boolean,
    tasks: List<TaskWithStatus>,
    onAddForDay: () -> Unit,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    FrostedGlassCard(
        cornerRadius = 18.dp,
        borderColor = if (isToday) atmo.accentCobalt.copy(alpha = 0.65f) else atmo.cardBorder,
        modifier = modifier.testTag("weekly_hub_day_$dayIso"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = DateTimeHelper.formatIndonesianFullDate(dayIso),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isToday) {
                        Surface(
                            color = atmo.accentCobalt,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Hari Ini",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = atmo.onAccentCobalt,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                BouncyIconButton(
                    onClick = onAddForDay,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Tambah jadwal untuk ${DateTimeHelper.formatShortDate(dayIso)}",
                        tint = atmo.accentCobalt,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "Belum ada jadwal untuk hari ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { item ->
                        CompactMatrixTaskRow(
                            item = item,
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedMonthlyCalendarCard(
    visibleMonthAnchor: String,
    selectedDateIso: String,
    todayIso: String,
    tasks: List<TaskWithStatus>,
    photoRecaps: Map<String, PhotoRecap>,
    onChangeMonth: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    onJumpToday: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val palette = LocalStatusPalette.current
    val cells = remember(visibleMonthAnchor) { DateTimeHelper.monthGridCells(visibleMonthAnchor) }

    FrostedGlassCard(cornerRadius = 18.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BouncyIconButton(
                    onClick = {
                        haptics.tapTick()
                        onChangeMonth(-1)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DateTimeHelper.formatMonthYear(visibleMonthAnchor),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (selectedDateIso != todayIso) {
                        BouncyTextButton(
                            onClick = {
                                haptics.tapSoft()
                                onJumpToday()
                            },
                        ) {
                            Text("Ke Hari Ini", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                BouncyIconButton(
                    onClick = {
                        haptics.tapTick()
                        onChangeMonth(1)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    week.forEach { cell ->
                        if (cell.dateIso == null || cell.dayOfMonth == null) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(0.88f))
                        } else {
                            val cellDate = cell.dateIso
                            val recap = photoRecaps[cellDate]
                            val thumbPath = recap?.imagePaths?.firstOrNull()?.takeIf { File(it).exists() }
                            val dayTasks = tasks.filter { it.task.dateIso == cellDate }
                            val isSelected = cellDate == selectedDateIso
                            val isToday = cellDate == todayIso
                            val shape = RoundedCornerShape(12.dp)
                            val dotColor = dayTasks.firstOrNull { !it.task.isCompleted }?.let {
                                palette.colorsFor(it.status).accent
                            } ?: if (dayTasks.isNotEmpty()) palette.greenAccent else null

                            val cellBg by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                } else {
                                    atmo.cardAltBg.copy(alpha = 0.58f)
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "matrix_cal_bg_$cellDate",
                            )
                            val cellScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.04f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                                label = "matrix_cal_scale_$cellDate",
                            )

                            val interaction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.88f)
                                    .graphicsLayer {
                                        scaleX = cellScale
                                        scaleY = cellScale
                                    }
                                    .bouncyPress(interaction, pressedScale = 0.90f)
                                    .clip(shape)
                                    .background(cellBg)
                                    .border(
                                        width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 1.dp,
                                        color = when {
                                            isSelected -> atmo.accentMint
                                            isToday -> atmo.accentCobalt
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = shape,
                                    )
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null,
                                    ) {
                                        haptics.tapSoft()
                                        onSelectDate(cellDate)
                                    }
                                    .testTag("matrix_calendar_day_$cellDate"),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (thumbPath != null) {
                                    AsyncImage(
                                        model = File(thumbPath),
                                        contentDescription = "Foto rekap ${cell.dayOfMonth}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f)),
                                    )
                                    Surface(
                                        color = Color(0xCC0E121B),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp),
                                    ) {
                                        Text(
                                            text = cell.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                } else {
                                    Text(
                                        text = cell.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = when {
                                            isSelected -> atmo.accentMint
                                            isToday -> atmo.accentCobalt
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }

                                if (dotColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(5.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(dotColor),
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
private fun MonthlyDayDetailCard(
    dateIso: String,
    recap: PhotoRecap?,
    tasks: List<TaskWithStatus>,
    onOpenRecapForDate: () -> Unit,
    onAddTaskForDate: () -> Unit,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val photos = recap?.imagePaths?.filter { File(it).exists() }.orEmpty()

    FrostedGlassCard(
        cornerRadius = 18.dp,
        modifier = Modifier.testTag("matrix_monthly_day_detail"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeHelper.formatIndonesianFullDate(dateIso),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${tasks.size} jadwal • ${photos.size} foto rekap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BouncyOutlinedButton(
                    onClick = {
                        haptics.tapSoft()
                        onOpenRecapForDate()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("matrix_open_recap_date_button"),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Rekap")
                }
            }

            if (photos.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    photos.take(4).forEach { path ->
                        AsyncImage(
                            model = File(path),
                            contentDescription = "Foto rekap tanggal $dateIso",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(84.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tapSoft()
                                    onOpenRecapForDate()
                                },
                        )
                    }
                }
            }

            if (recap != null && (recap.title.isNotBlank() || recap.caption.isNotBlank())) {
                Surface(
                    color = atmo.cardAltBg,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (recap.title.isNotBlank()) {
                            Text(
                                text = recap.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (recap.caption.isNotBlank()) {
                            Text(
                                text = recap.caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Acara & Tugas Hari Ini",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                BouncyTextButton(
                    onClick = {
                        haptics.tapSoft()
                        onAddTaskForDate()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah")
                }
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "Tidak ada jadwal di tanggal ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { item ->
                        CompactMatrixTaskRow(
                            item = item,
                            onToggleComplete = onToggleComplete,
                            onMoveToTomorrow = onMoveToTomorrow,
                            onChangeQuadrant = onChangeQuadrant,
                            onEditTask = onEditTask,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactMatrixTaskRow(
    item: TaskWithStatus,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onChangeQuadrant: (ScheduleTask, PriorityQuadrant) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
) {
    val task = item.task
    val haptics = LocalHapticFeedback.current
    val (_, _, qAccent) = quadrantThemeColors(task.quadrant)
    var menuExpanded by remember { mutableStateOf(false) }
    val rowInteraction = remember { MutableInteractionSource() }

    Surface(
        onClick = {
            haptics.tapSoft()
            onEditTask(task)
        },
        interactionSource = rowInteraction,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.bouncyPress(rowInteraction, pressedScale = 0.97f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(qAccent),
            )
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    if (!task.isCompleted) haptics.tapConfirm() else haptics.tapSoft()
                    onToggleComplete(task)
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay),
                        style = MonoSmallStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusBadge(status = item.status)
                    QuadrantChip(quadrant = task.quadrant)
                }
            }

            BouncyIconButton(
                onClick = {
                    haptics.tapSoft()
                    onMoveToTomorrow(task)
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Pindah ke Besok",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            Box {
                BouncyIconButton(
                    onClick = {
                        haptics.tapSoft()
                        menuExpanded = true
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Pindah kuadran",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    PriorityQuadrant.entries.filter { it != task.quadrant }.forEach { target ->
                        DropdownMenuItem(
                            text = { Text("Pindah ke ${target.shortCode}: ${target.titleId}") },
                            onClick = {
                                haptics.tapSoft()
                                menuExpanded = false
                                onChangeQuadrant(task, target)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun quadrantThemeColors(quadrant: PriorityQuadrant): Triple<Color, Color, Color> {
    val palette = LocalStatusPalette.current
    return when (quadrant) {
        PriorityQuadrant.Q1_IMPORTANT_URGENT -> Triple(palette.redBg, palette.redFg, palette.redAccent)
        PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> Triple(palette.blueBg, palette.blueFg, palette.blueAccent)
        PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> Triple(palette.yellowBg, palette.yellowFg, palette.yellowAccent)
        PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> Triple(palette.greenBg, palette.greenFg, palette.greenAccent)
    }
}
