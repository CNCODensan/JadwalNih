package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoRecap
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.ui.JadwalNihUiState
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.RingkasanCepatCard
import com.example.ui.components.ScheduleTaskCard
import com.example.ui.components.colorsFor
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle

@Composable
fun JadwalScreen(
    state: JadwalNihUiState,
    onSelectScope: (ScheduleScope) -> Unit,
    onSelectDate: (String) -> Unit,
    onStepDate: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectStatusFilter: (TaskStatus?) -> Unit,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onExportCalendar: (ScheduleTask) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    onDeleteTask: (ScheduleTask) -> Unit,
    onOpenFocusTab: () -> Unit,
    onOpenRecapTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayIso = DateTimeHelper.todayIso()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "quick_glance") {
            RingkasanCepatCard(
                summary = state.quickGlance,
                focusTimer = state.focusTimer,
                onOpenFocus = onOpenFocusTab,
                onOpenRecap = onOpenRecapTab,
            )
        }

        item(key = "scope_selector") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_scope_segmented"),
            ) {
                ScheduleScope.entries.forEachIndexed { index, scope ->
                    SegmentedButton(
                        selected = state.selectedScope == scope,
                        onClick = { onSelectScope(scope) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ScheduleScope.entries.size,
                        ),
                        modifier = Modifier.testTag("scope_tab_${scope.name.lowercase()}"),
                    ) {
                        Text(scope.label)
                    }
                }
            }
        }

        item(key = "date_navigator") {
            FrostedGlassCard(cornerRadius = 18.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = { onStepDate(-1) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Periode sebelumnya",
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val titleText = when (state.selectedScope) {
                                ScheduleScope.DAILY -> DateTimeHelper.formatIndonesianFullDate(state.selectedDateIso)
                                ScheduleScope.WEEKLY -> {
                                    val days = DateTimeHelper.weekDaysFor(state.selectedDateIso)
                                    "${DateTimeHelper.formatShortDate(days.first())} - ${DateTimeHelper.formatShortDate(days.last())}"
                                }
                                ScheduleScope.MONTHLY -> DateTimeHelper.formatMonthYear(state.selectedDateIso)
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (state.selectedDateIso != todayIso) {
                                TextButton(onClick = onJumpToToday) {
                                    Text("Ke Hari Ini", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        IconButton(
                            onClick = { onStepDate(1) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Periode berikutnya",
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.selectedScope != ScheduleScope.MONTHLY) {
                        WeekStrip(
                            anchorIso = state.selectedDateIso,
                            todayIso = todayIso,
                            tasks = state.allTasksWithStatus,
                            recaps = state.photoRecaps,
                            onSelectDate = {
                                onSelectDate(it)
                                if (state.selectedScope == ScheduleScope.WEEKLY) {
                                    onSelectScope(ScheduleScope.DAILY)
                                }
                            },
                        )
                    }

                    AnimatedVisibility(visible = state.selectedScope == ScheduleScope.MONTHLY) {
                        MonthCalendarGrid(
                            anchorIso = state.selectedDateIso,
                            todayIso = todayIso,
                            tasks = state.allTasksWithStatus,
                            recaps = state.photoRecaps,
                            onSelectDate = {
                                onSelectDate(it)
                                onSelectScope(ScheduleScope.DAILY)
                            },
                        )
                    }
                }
            }
        }

        item(key = "status_filters") {
            StatusFilterRow(
                selectedStatus = state.statusFilter,
                onSelectStatus = onSelectStatusFilter,
            )
        }

        if (state.filteredScheduleTasks.isEmpty()) {
            item(key = "empty_schedule") {
                FrostedGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = "Tidak ada jadwal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp),
                        )
                        Text(
                            text = if (state.statusFilter != null) {
                                "Belum ada yang statusnya \"${state.statusFilter.labelId}\""
                            } else {
                                "Jadwal masih kosong"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Ketuk + di kanan bawah buat nambah jadwal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(
                items = state.filteredScheduleTasks,
                key = { it.task.id },
            ) { item ->
                ScheduleTaskCard(
                    item = item,
                    onToggleComplete = onToggleComplete,
                    onMoveToTomorrow = onMoveToTomorrow,
                    onExportCalendar = onExportCalendar,
                    onEdit = onEditTask,
                    onDelete = onDeleteTask,
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(
    anchorIso: String,
    todayIso: String,
    tasks: List<TaskWithStatus>,
    recaps: Map<String, PhotoRecap>,
    onSelectDate: (String) -> Unit,
) {
    val weekDays = DateTimeHelper.weekDaysFor(anchorIso)
    val palette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        weekDays.forEach { dayIso ->
            val isSelected = dayIso == anchorIso
            val isToday = dayIso == todayIso
            val dayTasks = tasks.filter { it.task.dateIso == dayIso }
            val hasRecap = recaps[dayIso]?.imagePaths?.isNotEmpty() == true
            val dotColor = dayTasks.firstOrNull { !it.task.isCompleted }?.let {
                palette.colorsFor(it.status).accent
            } ?: if (dayTasks.isNotEmpty()) palette.greenAccent else null

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        when {
                            isSelected -> Modifier.background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(atmo.heroGradStart, atmo.heroGradEnd),
                                ),
                            )
                            isToday -> Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                            else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        },
                    )
                    .clickable { onSelectDate(dayIso) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = DateTimeHelper.formatDayShortLabel(dayIso),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = DateTimeHelper.dayOfMonthNumber(dayIso).toString(),
                        style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.height(6.dp),
                    ) {
                        if (dotColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else dotColor),
                            )
                        }
                        if (hasRecap) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFE9D5FF) else MaterialTheme.colorScheme.secondary),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    anchorIso: String,
    todayIso: String,
    tasks: List<TaskWithStatus>,
    recaps: Map<String, PhotoRecap>,
    onSelectDate: (String) -> Unit,
) {
    val cells = DateTimeHelper.monthGridCells(anchorIso)
    val palette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val headers = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            headers.forEach { h ->
                Text(
                    text = h,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        cells.chunked(7).forEach { weekRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                weekRow.forEach { cell ->
                    if (cell.dateIso == null || cell.dayOfMonth == null) {
                        Spacer(modifier = Modifier.weight(1f).height(44.dp))
                    } else {
                        val isSelected = cell.dateIso == anchorIso
                        val isToday = cell.dateIso == todayIso
                        val dayTasks = tasks.filter { it.task.dateIso == cell.dateIso }
                        val hasRecap = recaps[cell.dateIso]?.imagePaths?.isNotEmpty() == true
                        val dotColor = dayTasks.firstOrNull { !it.task.isCompleted }?.let {
                            palette.colorsFor(it.status).accent
                        } ?: if (dayTasks.isNotEmpty()) palette.greenAccent else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    when {
                                        isSelected -> Modifier.background(
                                            brush = Brush.linearGradient(listOf(atmo.heroGradStart, atmo.heroGradEnd)),
                                        )
                                        isToday -> Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                        else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    },
                                )
                                .clickable { onSelectDate(cell.dateIso) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = cell.dayOfMonth.toString(),
                                    style = MonoSmallStyle,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.height(6.dp),
                                ) {
                                    if (dotColor != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else dotColor),
                                        )
                                    }
                                    if (hasRecap) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFFE9D5FF) else MaterialTheme.colorScheme.secondary),
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
private fun StatusFilterRow(
    selectedStatus: TaskStatus?,
    onSelectStatus: (TaskStatus?) -> Unit,
) {
    val palette = LocalStatusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onSelectStatus(null) },
            label = { Text("Semua") },
        )
        TaskStatus.entries.forEach { status ->
            val colors = palette.colorsFor(status)
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onSelectStatus(status) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                },
                label = { Text(status.labelId) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.bg,
                    selectedLabelColor = colors.fg,
                ),
            )
        }
    }
}
