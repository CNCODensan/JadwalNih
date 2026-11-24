package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.DateTimeHelper
import com.example.domain.HolidayInfo
import com.example.domain.ScheduleTask
import com.example.domain.TaskIconCatalog
import com.example.ui.JadwalNihUiState
import com.example.ui.components.BirthdayTodayMiniCard
import com.example.ui.components.BouncyButton
import com.example.ui.components.BouncyFilterChip
import com.example.ui.components.BouncyIconButton
import com.example.ui.components.BouncyOutlinedButton
import com.example.ui.components.BouncyTextButton
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.ScheduleTaskCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.colorsFor
import com.example.ui.components.tapSoft
import com.example.ui.components.tapTick
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle
import java.io.File

private enum class KalenderFilterMode(val labelId: String) {
    ALL("Semua"),
    TASKS("Jadwal"),
    HOLIDAYS("Libur & Nasional"),
    RECAPS("Rekap Foto"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KalenderScreen(
    state: JadwalNihUiState,
    onSelectDate: (String) -> Unit,
    onAddTaskForDate: (String) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    onToggleComplete: (ScheduleTask) -> Unit,
    onToggleSubTask: (ScheduleTask, String) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onExportCalendar: (ScheduleTask) -> Unit,
    onDeleteTask: (ScheduleTask) -> Unit,
    onDeleteRecurringSeries: (ScheduleTask) -> Unit,
    onOverrideAttended: (ScheduleTask) -> Unit,
    onOpenRecapForDate: (String) -> Unit,
    onOpenBirthdayCelebration: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val statusPalette = LocalStatusPalette.current
    val spec = LocalAdaptiveSpec.current
    val haptics = LocalHapticFeedback.current
    val todayIso = state.currentDateIso.ifBlank { DateTimeHelper.todayIso() }

    var visibleMonthAnchor by remember(state.selectedDateIso) {
        mutableStateOf(state.selectedDateIso)
    }
    LaunchedEffect(state.selectedDateIso) {
        if (state.selectedDateIso.take(7) != visibleMonthAnchor.take(7)) {
            visibleMonthAnchor = state.selectedDateIso
        }
    }

    var filterMode by remember { mutableStateOf(KalenderFilterMode.ALL) }

    val holidaysByDate = remember(state.allHolidays) {
        state.allHolidays.groupBy { it.dateIso }
    }
    val tasksByDate = remember(state.allTasksWithStatus) {
        state.allTasksWithStatus.groupBy { it.task.dateIso }
    }
    val selectedDayHolidays = holidaysByDate[state.selectedDateIso].orEmpty()
    val selectedDayTasks = remember(state.allTasksWithStatus, state.selectedDateIso) {
        tasksByDate[state.selectedDateIso].orEmpty()
            .sortedBy { it.task.startMinuteOfDay }
    }
    val selectedDayRecap = state.photoRecaps[state.selectedDateIso]
    val isSelectedUserBirthday = remember(state.birthDateIso, state.selectedDateIso) {
        state.birthDateIso.isNotBlank() && DateTimeHelper.isSameMonthAndDay(state.birthDateIso, state.selectedDateIso)
    }

    val monthPrefix = visibleMonthAnchor.take(7)
    val monthHolidays = remember(state.allHolidays, monthPrefix) {
        state.allHolidays.filter { it.dateIso.startsWith(monthPrefix) }.sortedBy { it.dateIso }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("kalender_screen"),
        contentPadding = PaddingValues(
            start = spec.screenHorizontalPadding,
            end = spec.screenHorizontalPadding,
            top = 6.dp,
            bottom = spec.fabContentClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "kalender_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kalender Terpadu",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Jadwal, libur merah, hari nasional, ulang tahun & rekap foto dalam satu kalender.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.selectedDateIso != todayIso || visibleMonthAnchor.take(7) != todayIso.take(7)) {
                        BouncyTextButton(
                            onClick = {
                                haptics.tapSoft()
                                visibleMonthAnchor = todayIso
                                onSelectDate(todayIso)
                            },
                            modifier = Modifier.testTag("kalender_today_button"),
                        ) {
                            Text("Hari Ini")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KalenderFilterMode.entries.forEach { mode ->
                        BouncyFilterChip(
                            selected = filterMode == mode,
                            onClick = {
                                haptics.tapTick()
                                filterMode = mode
                            },
                            label = { Text(mode.labelId) },
                            modifier = Modifier.testTag("kalender_filter_${mode.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        item(key = "kalender_month_grid") {
            FrostedGlassCard(cornerRadius = 22.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spec.cardPadding),
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
                                visibleMonthAnchor = DateTimeHelper.addMonths(visibleMonthAnchor, -1)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("kalender_prev_month_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                        }
                        Text(
                            text = DateTimeHelper.formatMonthYear(visibleMonthAnchor),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        BouncyIconButton(
                            onClick = {
                                haptics.tapTick()
                                visibleMonthAnchor = DateTimeHelper.addMonths(visibleMonthAnchor, 1)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("kalender_next_month_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEachIndexed { idx, label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (idx == 6) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    val cells = remember(visibleMonthAnchor) {
                        DateTimeHelper.monthGridCells(visibleMonthAnchor)
                    }
                    cells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            week.forEachIndexed { dayIndex, cell ->
                                if (cell.dateIso == null || cell.dayOfMonth == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.9f),
                                    )
                                } else {
                                    val cellDate = cell.dateIso
                                    val cellHolidays = holidaysByDate[cellDate].orEmpty()
                                    val hasPublicHoliday = cellHolidays.any { !it.isNationalObservance }
                                    val hasNationalObservance = cellHolidays.any { it.isNationalObservance }
                                    val dayTasks = tasksByDate[cellDate].orEmpty()
                                    val hasBirthday = (state.birthDateIso.isNotBlank() &&
                                        DateTimeHelper.isSameMonthAndDay(state.birthDateIso, cellDate)) ||
                                        dayTasks.any {
                                            it.task.iconKey == "cake" ||
                                                (it.task.iconKey.isBlank() && TaskIconCatalog.autoDetectIconKey(it.task.title) == "cake")
                                        }
                                    val recap = state.photoRecaps[cellDate]
                                    val hasRecap = recap?.imagePaths?.isNotEmpty() == true
                                    val showTaskDot = (filterMode == KalenderFilterMode.ALL || filterMode == KalenderFilterMode.TASKS) && dayTasks.isNotEmpty()
                                    val showHolidayDot = (filterMode == KalenderFilterMode.ALL || filterMode == KalenderFilterMode.HOLIDAYS) && cellHolidays.isNotEmpty()
                                    val showRecapBadge = (filterMode == KalenderFilterMode.ALL || filterMode == KalenderFilterMode.RECAPS) && hasRecap

                                    val isSelected = cellDate == state.selectedDateIso
                                    val isToday = cellDate == todayIso
                                    val isSunday = dayIndex == 6
                                    val shape = RoundedCornerShape(12.dp)

                                    val cellBg by animateColorAsState(
                                        targetValue = when {
                                            isSelected -> atmo.accentCobalt
                                            hasPublicHoliday -> statusPalette.redBg.copy(alpha = 0.65f)
                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                            else -> atmo.cardAltBg.copy(alpha = 0.55f)
                                        },
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "kalender_cell_$cellDate",
                                    )

                                    val borderColor = when {
                                        isSelected -> atmo.accentMint
                                        hasNationalObservance && !hasPublicHoliday -> Color(0xFFF59E0B)
                                        hasPublicHoliday -> Color(0xFFDC2626).copy(alpha = 0.65f)
                                        isToday -> atmo.accentCobalt
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    }

                                    val numberColor = when {
                                        isSelected -> atmo.onAccentCobalt
                                        hasPublicHoliday || isSunday -> if (atmo.isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                        hasNationalObservance -> if (atmo.isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    val interaction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.9f)
                                            .bouncyPress(interaction, pressedScale = 0.90f)
                                            .clip(shape)
                                            .background(cellBg)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = borderColor,
                                                shape = shape,
                                            )
                                            .clickable(
                                                interactionSource = interaction,
                                                indication = null,
                                            ) {
                                                haptics.tapSoft()
                                                onSelectDate(cellDate)
                                            }
                                            .testTag("kalender_day_$cellDate"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                text = cell.dayOfMonth.toString(),
                                                style = MonoSmallStyle.copy(
                                                    fontWeight = if (isSelected || isToday || hasPublicHoliday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                ),
                                                color = numberColor,
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.height(6.dp),
                                            ) {
                                                if (showHolidayDot) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (hasPublicHoliday) Color(0xFFDC2626) else Color(0xFFF59E0B),
                                                            ),
                                                    )
                                                }
                                                if (hasBirthday) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFEC4899)),
                                                    )
                                                }
                                                if (showTaskDot) {
                                                    val taskDotColor = dayTasks.firstOrNull { !it.task.isCompleted }?.let {
                                                        statusPalette.colorsFor(it.status).accent
                                                    } ?: statusPalette.greenAccent
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else taskDotColor),
                                                    )
                                                }
                                                if (showRecapBadge) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(atmo.accentMint),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        KalenderLegendItem(Color(0xFFDC2626), "Libur Merah")
                        KalenderLegendItem(Color(0xFFF59E0B), "Hari Nasional (Emas)")
                        KalenderLegendItem(Color(0xFFEC4899), "Ulang Tahun")
                        KalenderLegendItem(atmo.accentCobalt, "Jadwal")
                        KalenderLegendItem(atmo.accentMint, "Rekap Foto")
                    }
                }
            }
        }

        // Selected Day Unified Detail Section
        item(key = "kalender_selected_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeHelper.formatIndonesianFullDate(state.selectedDateIso),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${selectedDayTasks.size} jadwal • ${selectedDayHolidays.size} momen nasional • ${selectedDayRecap?.imagePaths?.size ?: 0} foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BouncyButton(
                    onClick = {
                        haptics.tapSoft()
                        onAddTaskForDate(state.selectedDateIso)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = atmo.accentCobalt,
                        contentColor = atmo.onAccentCobalt,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("kalender_add_task_button"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isSelectedUserBirthday) {
            item(key = "kalender_user_birthday") {
                BirthdayTodayMiniCard(
                    userName = state.userName,
                    stats = state.personalBirthdayStats,
                    onOpenCelebration = onOpenBirthdayCelebration,
                )
            }
        }

        if ((filterMode == KalenderFilterMode.ALL || filterMode == KalenderFilterMode.HOLIDAYS) && selectedDayHolidays.isNotEmpty()) {
            items(
                items = selectedDayHolidays,
                key = { "holiday_${it.dateIso}_${it.name}" },
            ) { holiday ->
                KalenderHolidayCard(holiday = holiday)
            }
        }

        if (filterMode == KalenderFilterMode.ALL || filterMode == KalenderFilterMode.TASKS) {
            if (selectedDayTasks.isEmpty() && filterMode == KalenderFilterMode.TASKS) {
                item(key = "kalender_no_tasks") {
                    FrostedGlassCard(cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Belum ada jadwal pada tanggal ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = selectedDayTasks,
                    key = { "kalender_task_${it.task.id}" },
                ) { item ->
                    ScheduleTaskCard(
                        item = item,
                        onToggleComplete = onToggleComplete,
                        onMoveToTomorrow = onMoveToTomorrow,
                        onExportCalendar = onExportCalendar,
                        onEdit = onEditTask,
                        onDelete = onDeleteTask,
                        onDeleteRecurringSeries = onDeleteRecurringSeries,
                        conflictInfo = state.conflictsByTaskId[item.task.id],
                        onToggleSubTask = onToggleSubTask,
                        onOverrideAttended = onOverrideAttended,
                        todayIso = todayIso,
                        nowMinuteOfDay = state.currentMinuteOfDay,
                    )
                }
            }
        }

        if (filterMode == KalenderFilterMode.ALL || filterMode == KalenderFilterMode.RECAPS) {
            val recap = selectedDayRecap
            val validPaths = recap?.imagePaths?.filter { File(it).exists() }.orEmpty()
            if (validPaths.isNotEmpty() || filterMode == KalenderFilterMode.RECAPS) {
                item(key = "kalender_day_recap") {
                    FrostedGlassCard(
                        cornerRadius = 20.dp,
                        borderColor = atmo.accentMint.copy(alpha = 0.5f),
                        modifier = Modifier.testTag("kalender_recap_card"),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = atmo.accentMint,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = recap?.title?.ifBlank { "Rekap Foto Hari Ini" } ?: "Belum Ada Rekap Foto",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                BouncyOutlinedButton(
                                    onClick = {
                                        haptics.tapSoft()
                                        onOpenRecapForDate(state.selectedDateIso)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (validPaths.isEmpty()) "Tambah Foto" else "Buka Rekap")
                                }
                            }

                            if (recap?.locationName?.isNotBlank() == true) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = atmo.accentMint,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = recap.locationName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = atmo.accentMint,
                                    )
                                }
                            }

                            if (validPaths.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    validPaths.take(4).forEach { path ->
                                        AsyncImage(
                                            model = File(path),
                                            contentDescription = "Foto rekap ${state.selectedDateIso}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(72.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onOpenRecapForDate(state.selectedDateIso) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filterMode == KalenderFilterMode.HOLIDAYS && monthHolidays.isNotEmpty()) {
            item(key = "kalender_month_holidays_header") {
                Text(
                    text = "Daftar Hari Libur & Peringatan (${DateTimeHelper.formatMonthYear(visibleMonthAnchor)})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            items(
                items = monthHolidays,
                key = { "month_holiday_${it.dateIso}_${it.name}" },
            ) { holiday ->
                KalenderHolidayCard(
                    holiday = holiday,
                    showDate = true,
                    onClick = { onSelectDate(holiday.dateIso) },
                )
            }
        }
    }
}

@Composable
private fun KalenderLegendItem(color: Color, label: String) {
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
private fun KalenderHolidayCard(
    holiday: HolidayInfo,
    showDate: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val isNational = holiday.isNationalObservance
    val badgeBg = if (isNational) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
    val badgeFg = if (isNational) Color(0xFF92400E) else Color(0xFF991B1B)
    val borderColor = if (isNational) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFFDC2626).copy(alpha = 0.6f)

    FrostedGlassCard(
        cornerRadius = 18.dp,
        borderColor = borderColor,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .testTag("kalender_holiday_${holiday.dateIso}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = badgeBg,
                shape = CircleShape,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Celebration,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = if (isNational) "Hari Peringatan Nasional" else "Libur Nasional",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    if (showDate) {
                        Text(
                            text = DateTimeHelper.formatShortDate(holiday.dateIso),
                            style = MonoSmallStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = holiday.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = holiday.bannerGreeting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
