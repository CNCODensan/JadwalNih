package com.example.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.AttendanceStatus
import com.example.domain.DateTimeHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleTask
import com.example.domain.SubTaskItem
import com.example.domain.TaskDraft
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    initialTask: ScheduleTask?,
    initialDraft: TaskDraft? = null,
    defaultDateIso: String,
    defaultStartHour: Int? = null,
    existingTasks: List<ScheduleTask> = emptyList(),
    onSaveDraftAndDismiss: (TaskDraft) -> Unit,
    onDiscardAndDismiss: () -> Unit,
    onSave: (ScheduleTask) -> Unit,
    onNotifyPermissionDenied: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val statusPalette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultStart = remember(initialTask, initialDraft, defaultStartHour) {
        initialTask?.startMinuteOfDay
            ?: initialDraft?.startMinuteOfDay
            ?: defaultStartHour?.let { it.coerceIn(0, 22) * 60 }
            ?: ((DateTimeHelper.nowMinuteOfDay() / 30 + 1) * 30).coerceAtMost(21 * 60)
    }
    val defaultEnd = remember(initialTask, initialDraft, defaultStart) {
        initialTask?.endMinuteOfDay
            ?: initialDraft?.endMinuteOfDay
            ?: (defaultStart + 60).coerceAtMost(23 * 60 + 45)
    }

    var title by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.title ?: initialDraft?.title ?: "")
    }
    var dateIso by remember(initialTask, initialDraft, defaultDateIso) {
        mutableStateOf(initialTask?.dateIso ?: initialDraft?.dateIso ?: defaultDateIso)
    }
    var startMin by remember(initialTask, initialDraft, defaultStart) {
        mutableIntStateOf(defaultStart)
    }
    var endMin by remember(initialTask, initialDraft, defaultEnd) {
        mutableIntStateOf(defaultEnd)
    }
    var quadrant by remember(initialTask, initialDraft) {
        mutableStateOf(
            initialTask?.quadrant
                ?: initialDraft?.quadrant
                ?: PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
        )
    }
    var isFlexible by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.isFlexible ?: initialDraft?.isFlexible ?: false)
    }
    var recurrence by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.recurrence ?: initialDraft?.recurrence ?: RecurrenceRule.NONE)
    }
    var reminderEnabled by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.reminderEnabled ?: initialDraft?.reminderEnabled ?: false)
    }
    var reminderLeadMinutes by remember(initialTask, initialDraft) {
        mutableIntStateOf(initialTask?.reminderLeadMinutes ?: initialDraft?.reminderLeadMinutes ?: 10)
    }
    var subtasks by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.subtasks ?: initialDraft?.subtasks ?: emptyList())
    }
    var isSubtasksTodoMode by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.isSubtasksTodoMode ?: initialDraft?.isSubtasksTodoMode ?: false)
    }
    var newSubtaskInput by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var validationShakeTrigger by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }

    val draftConflicts = remember(dateIso, startMin, endMin, initialTask, existingTasks) {
        ScheduleAnalyzer.detectConflictsForDraft(
            dateIso = dateIso,
            startMinuteOfDay = startMin,
            endMinuteOfDay = endMin,
            excludeTaskId = initialTask?.id ?: -1L,
            existingTasks = existingTasks,
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            reminderEnabled = true
        } else {
            reminderEnabled = false
            onNotifyPermissionDenied()
        }
    }

    fun buildCurrentDraft(): TaskDraft = TaskDraft(
        editingTaskId = initialTask?.id,
        title = title,
        dateIso = dateIso,
        startMinuteOfDay = startMin,
        endMinuteOfDay = endMin,
        quadrant = quadrant,
        isFlexible = isFlexible,
        recurrence = recurrence,
        reminderEnabled = reminderEnabled,
        reminderLeadMinutes = reminderLeadMinutes,
        subtasks = subtasks,
        isSubtasksTodoMode = isSubtasksTodoMode,
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeHelper.isoToUtcDatePickerMillis(dateIso),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                BouncyTextButton(
                    onClick = {
                        haptics.tapConfirm()
                        datePickerState.selectedDateMillis?.let { millis ->
                            dateIso = DateTimeHelper.utcDatePickerMillisToIso(millis)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                BouncyTextButton(
                    onClick = {
                        haptics.tapSoft()
                        showDatePicker = false
                    },
                ) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            val hasMeaningfulInput = title.isNotBlank() || subtasks.isNotEmpty()
            if (initialTask == null && hasMeaningfulInput) {
                onSaveDraftAndDismiss(buildCurrentDraft())
            } else {
                onDiscardAndDismiss()
            }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = atmo.sheetBg,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = atmo.sheetScrim,
        tonalElevation = 0.dp,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .height(1.dp)
                        .background(atmo.cardBorder),
                )
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 10.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.outline),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(atmo.sheetBg)
                .verticalScroll(rememberScrollState())
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (initialTask == null) {
                        if (initialDraft != null) "Lanjutkan Draft" else "Tambah Jadwal"
                    } else {
                        "Edit Jadwal"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    formError = null
                },
                label = { Text("Judul Kegiatan") },
                placeholder = { Text("Misal: Belajar Compose atau rapat tim") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeOnTrigger(validationShakeTrigger)
                    .testTag("task_title_input"),
            )

            // Date selector (Hari Ini, Besok + Pilih Tanggal Manual via DatePicker)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tanggal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val today = DateTimeHelper.todayIso()
                val tomorrow = DateTimeHelper.addDays(today, 1)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BouncyFilterChip(
                        selected = dateIso == today,
                        onClick = {
                            haptics.tapSoft()
                            dateIso = today
                        },
                        label = { Text("Hari Ini") },
                    )
                    BouncyFilterChip(
                        selected = dateIso == tomorrow,
                        onClick = {
                            haptics.tapSoft()
                            dateIso = tomorrow
                        },
                        label = { Text("Besok") },
                    )
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            showDatePicker = true
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pick_manual_date_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Pilih tanggal dari kalender",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih Tanggal")
                    }
                }
                Surface(
                    onClick = {
                        haptics.tapSoft()
                        showDatePicker = true
                    },
                    color = atmo.sheetCardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Tanggal terpilih",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = DateTimeHelper.formatIndonesianFullDate(dateIso),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "Ubah",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Start & End Time Pickers (Durasi otomatis)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    haptics.tapTick()
                                    val newStart = hour * 60 + minute
                                    startMin = newStart
                                    if (endMin <= newStart) {
                                        endMin = (newStart + 30).coerceAtMost(23 * 60 + 59)
                                    }
                                    formError = null
                                },
                                startMin / 60,
                                startMin % 60,
                                true,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Mulai: ${DateTimeHelper.formatMinuteOfDay(startMin)}", style = MonoSmallStyle)
                    }

                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    haptics.tapTick()
                                    endMin = hour * 60 + minute
                                    formError = null
                                },
                                endMin / 60,
                                endMin % 60,
                                true,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Selesai: ${DateTimeHelper.formatMinuteOfDay(endMin)}", style = MonoSmallStyle)
                    }
                }

                val autoDurationMins = (endMin - startMin)
                if (autoDurationMins > 0) {
                    Text(
                        text = "Durasi otomatis: ${DateTimeHelper.formatTimelineDuration(autoDurationMins)}",
                        style = MonoSmallStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            AnimatedVisibility(
                visible = draftConflicts.isNotEmpty(),
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    color = statusPalette.redBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, statusPalette.redAccent.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Peringatan bentrok jam",
                            tint = statusPalette.redFg,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bentrok dengan: ${draftConflicts.joinToString { it.title }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = statusPalette.redFg,
                            )
                            Text(
                                text = "Tetap bisa disimpan kalau memang paralel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusPalette.redFg.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }

            // Prioritas: Grid 2x2 kotak persegi (1x1)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Prioritas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PriorityQuadrant.entries.chunked(2).forEach { rowQuadrants ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowQuadrants.forEach { q ->
                                val selected = quadrant == q
                                val (qSolidBg, qSolidFg, qSoftBg, qSoftFg) = when (q) {
                                    PriorityQuadrant.Q1_IMPORTANT_URGENT -> listOf(
                                        atmo.accentCoral,
                                        atmo.onAccentCoral,
                                        statusPalette.redBg,
                                        statusPalette.redFg,
                                    )
                                    PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT -> listOf(
                                        atmo.accentCobalt,
                                        atmo.onAccentCobalt,
                                        statusPalette.blueBg,
                                        statusPalette.blueFg,
                                    )
                                    PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT -> listOf(
                                        atmo.accentOrange,
                                        atmo.onAccentOrange,
                                        statusPalette.yellowBg,
                                        statusPalette.yellowFg,
                                    )
                                    PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT -> listOf(
                                        atmo.accentMint,
                                        atmo.onAccentMint,
                                        statusPalette.greenBg,
                                        statusPalette.greenFg,
                                    )
                                }
                                val qInteraction = remember(q) { MutableInteractionSource() }
                                val animatedCardColor by animateColorAsState(
                                    targetValue = if (selected) qSolidBg else qSoftBg,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "q_card_bg_${q.shortCode}",
                                )
                                val animatedContentColor by animateColorAsState(
                                    targetValue = if (selected) qSolidFg else qSoftFg,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "q_card_fg_${q.shortCode}",
                                )
                                val selectedLift by animateFloatAsState(
                                    targetValue = if (selected) 1.02f else 0.985f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                    label = "q_card_lift_${q.shortCode}",
                                )
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            scaleX = selectedLift
                                            scaleY = selectedLift
                                        }
                                        .bouncyPress(qInteraction, pressedScale = 0.94f)
                                        .clickable(
                                            interactionSource = qInteraction,
                                            indication = null,
                                        ) {
                                            haptics.tapSoft()
                                            quadrant = q
                                        }
                                        .testTag("priority_square_${q.shortCode}"),
                                    color = animatedCardColor,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        width = if (selected) 2.5.dp else 1.2.dp,
                                        color = qSolidBg,
                                    ),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = q.shortCode,
                                                style = MonoSmallStyle.copy(fontWeight = FontWeight.ExtraBold),
                                                color = animatedContentColor,
                                            )
                                            AnimatedVisibility(
                                                visible = selected,
                                                enter = scaleIn(
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                ) + fadeIn(),
                                                exit = scaleOut() + fadeOut(),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Terpilih",
                                                    tint = animatedContentColor,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.align(Alignment.BottomStart),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                text = q.chipLabelId,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = animatedContentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = q.subtitleId,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = animatedContentColor.copy(alpha = 0.85f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Langkah Kecil (Sub-tugas) + Opsi To-Do List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Langkah Kecil (Opsional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Mode To-Do List",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(
                            checked = isSubtasksTodoMode,
                            onCheckedChange = {
                                haptics.tapSoft()
                                isSubtasksTodoMode = it
                            },
                            modifier = Modifier.testTag("subtask_todo_mode_switch"),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newSubtaskInput,
                        onValueChange = { newSubtaskInput = it },
                        placeholder = { Text(if (isSubtasksTodoMode) "Tambah item to-do..." else "Misal: Siapkan bahan slide") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_subtask_input"),
                    )
                    BouncyFilledTonalIconButton(
                        onClick = {
                            val trimmed = newSubtaskInput.trim()
                            if (trimmed.isNotEmpty()) {
                                haptics.tapSoft()
                                subtasks = subtasks + SubTaskItem(
                                    id = "sub_${System.currentTimeMillis()}_${subtasks.size}",
                                    title = trimmed,
                                    isDone = false,
                                )
                                newSubtaskInput = ""
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_subtask_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah langkah")
                    }
                }

                subtasks.forEach { sub ->
                    Surface(
                        color = atmo.sheetCardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = sub.isDone,
                                onCheckedChange = { checked ->
                                    if (checked) haptics.tapConfirm() else haptics.tapSoft()
                                    subtasks = subtasks.map {
                                        if (it.id == sub.id) it.copy(isDone = checked) else it
                                    }
                                },
                            )
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                ),
                                color = if (sub.isDone) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                            BouncyIconButton(
                                onClick = {
                                    haptics.tapSoft()
                                    subtasks = subtasks.filterNot { it.id == sub.id }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Hapus langkah",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Recurrence
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ulangi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceRule.entries.forEach { rule ->
                        BouncyFilterChip(
                            selected = recurrence == rule,
                            onClick = {
                                haptics.tapSoft()
                                recurrence = rule
                            },
                            label = { Text(rule.labelId) },
                        )
                    }
                }
            }

            // Flexible Time Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Waktu Fleksibel",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Tandai biru kalau jamnya longgar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isFlexible,
                    onCheckedChange = {
                        haptics.tapSoft()
                        isFlexible = it
                    },
                )
            }

            // Reminder Notification Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pengingat",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Kirim notif sebelum mulai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { checked ->
                        haptics.tapSoft()
                        if (!checked) {
                            reminderEnabled = false
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                reminderEnabled = true
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            reminderEnabled = true
                        }
                    },
                )
            }

            AnimatedVisibility(
                visible = reminderEnabled,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { mins ->
                        BouncyFilterChip(
                            selected = reminderLeadMinutes == mins,
                            onClick = {
                                haptics.tapTick()
                                reminderLeadMinutes = mins
                            },
                            label = { Text("-$mins mnt") },
                        )
                    }
                }
            }

            formError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BouncyOutlinedButton(
                    onClick = {
                        haptics.tapSoft()
                        onDiscardAndDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Batal")
                }
                BouncyButton(
                    onClick = {
                        val trimmed = title.trim()
                        val computedDuration = endMin - startMin
                        when {
                            trimmed.isEmpty() -> {
                                haptics.tapWarning()
                                validationShakeTrigger += 1
                                formError = "Isi judul dulu ya."
                            }
                            computedDuration <= 0 -> {
                                haptics.tapWarning()
                                validationShakeTrigger += 1
                                formError = "Jam selesai harus lebih akhir."
                            }
                            else -> {
                                haptics.tapConfirm()
                                onSave(
                                    ScheduleTask(
                                        id = initialTask?.id ?: 0L,
                                        title = trimmed,
                                        dateIso = dateIso,
                                        startMinuteOfDay = startMin,
                                        endMinuteOfDay = endMin,
                                        estimatedMinutes = computedDuration,
                                        quadrant = quadrant,
                                        isCompleted = initialTask?.isCompleted ?: false,
                                        isFlexible = isFlexible,
                                        recurrence = recurrence,
                                        reminderEnabled = reminderEnabled,
                                        reminderLeadMinutes = reminderLeadMinutes,
                                        completedAtEpochMs = initialTask?.completedAtEpochMs,
                                        createdAtEpochMs = initialTask?.createdAtEpochMs ?: System.currentTimeMillis(),
                                        subtasks = subtasks,
                                        attendanceStatus = initialTask?.attendanceStatus ?: AttendanceStatus.UNCONFIRMED,
                                        isSubtasksTodoMode = isSubtasksTodoMode,
                                    ),
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_task_button"),
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}
