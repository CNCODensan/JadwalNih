package com.example.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.AiBatchScheduleParser
import com.example.domain.AiConflictResolution
import com.example.domain.AttendanceStatus
import com.example.domain.DateTimeHelper
import com.example.domain.LocationHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleTask
import com.example.domain.SubTaskItem
import com.example.domain.TaskDraft
import com.example.domain.TaskIconCatalog
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
    onDraftChanged: (TaskDraft) -> Unit = {},
    onSaveDraftAndDismiss: (TaskDraft) -> Unit,
    onDiscardAndDismiss: () -> Unit,
    onSave: (ScheduleTask) -> Unit,
    onSaveBatch: (List<ScheduleTask>, List<String>) -> Unit = { _, _ -> },
    onNotifyMessage: (String) -> Unit = {},
    onNotifyPermissionDenied: () -> Unit,
    defaultLocationName: String = "",
    defaultLocationLat: Double? = null,
    defaultLocationLng: Double? = null,
    onDetectLocation: (() -> Unit)? = null,
    onSaveBatchWithResolution: ((List<ScheduleTask>, List<String>, AiConflictResolution) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val statusPalette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultOtomatis = remember(initialTask, initialDraft, defaultStartHour) {
        when {
            initialTask != null -> false
            initialDraft != null -> initialDraft.isOtomatisMode
            defaultStartHour != null -> false
            else -> true
        }
    }

    var isOtomatisTab by remember(initialTask, initialDraft, defaultStartHour) {
        mutableStateOf(defaultOtomatis)
    }

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
        mutableIntStateOf(initialTask?.reminderLeadMinutes ?: initialDraft?.reminderLeadMinutes ?: 15)
    }
    var subtasks by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.subtasks ?: initialDraft?.subtasks ?: emptyList())
    }
    var isSubtasksTodoMode by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.isSubtasksTodoMode ?: initialDraft?.isSubtasksTodoMode ?: false)
    }
    var notes by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.notes ?: initialDraft?.notes ?: "")
    }
    var iconKey by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.iconKey ?: initialDraft?.iconKey ?: "")
    }
    var locationName by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.locationName ?: initialDraft?.locationName ?: "")
    }
    var locationLat by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.locationLat ?: initialDraft?.locationLat)
    }
    var locationLng by remember(initialTask, initialDraft) {
        mutableStateOf(initialTask?.locationLng ?: initialDraft?.locationLng)
    }
    var aiRawText by remember(initialTask, initialDraft) {
        mutableStateOf(initialDraft?.aiRawText ?: "")
    }
    var promptCopied by remember { mutableStateOf(false) }

    var newSubtaskInput by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var validationShakeTrigger by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var iconPickerTargetAiIdx by remember { mutableStateOf<Int?>(null) }
    var pendingDeleteSubtaskId by remember { mutableStateOf<String?>(null) }
    var pendingAiConflicts by remember { mutableStateOf<List<AiBatchScheduleParser.ImportedConflictItem>?>(null) }

    val draftConflicts = remember(dateIso, startMin, endMin, initialTask, existingTasks) {
        ScheduleAnalyzer.detectConflictsForDraft(
            dateIso = dateIso,
            startMinuteOfDay = startMin,
            endMinuteOfDay = endMin,
            excludeTaskId = initialTask?.id ?: -1L,
            existingTasks = existingTasks,
        )
    }

    val liveAiPreview = remember(aiRawText, dateIso, existingTasks) {
        if (aiRawText.isBlank()) {
            null
        } else {
            AiBatchScheduleParser.parse(
                rawText = aiRawText,
                defaultDateIso = dateIso,
                existingTasks = existingTasks,
            )
        }
    }

    var editableAiTasks by remember { mutableStateOf<List<ScheduleTask>>(emptyList()) }
    LaunchedEffect(liveAiPreview) {
        editableAiTasks = liveAiPreview?.takeIf { it.errorMessage == null }?.tasks.orEmpty()
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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            if (locationName.isBlank() && defaultLocationName.isNotBlank()) {
                locationName = defaultLocationName
                locationLat = defaultLocationLat
                locationLng = defaultLocationLng
            }
            onDetectLocation?.invoke()
        } else {
            onNotifyMessage("Izin lokasi ditolak. Kamu masih bisa isi tempat secara manual.")
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
        notes = notes,
        aiRawText = aiRawText,
        isOtomatisMode = isOtomatisTab,
        iconKey = iconKey,
        locationName = locationName,
        locationLat = locationLat,
        locationLng = locationLng,
    )

    // Real-time continuous auto-save of draft while typing/selecting
    LaunchedEffect(
        title,
        dateIso,
        startMin,
        endMin,
        quadrant,
        isFlexible,
        recurrence,
        reminderEnabled,
        reminderLeadMinutes,
        subtasks,
        isSubtasksTodoMode,
        notes,
        aiRawText,
        isOtomatisTab,
        iconKey,
        locationName,
    ) {
        if (initialTask == null) {
            onDraftChanged(buildCurrentDraft())
        }
    }

    fun handleCloseOrCancel() {
        val currentDraft = buildCurrentDraft()
        if (initialTask == null && currentDraft.hasMeaningfulContent()) {
            onSaveDraftAndDismiss(currentDraft)
        } else {
            onDiscardAndDismiss()
        }
    }

    if (showIconPicker) {
        val targetAiIdx = iconPickerTargetAiIdx
        val targetTitle = if (targetAiIdx != null) {
            editableAiTasks.getOrNull(targetAiIdx)?.title.orEmpty()
        } else {
            title
        }
        val targetCurrentKey = if (targetAiIdx != null) {
            editableAiTasks.getOrNull(targetAiIdx)?.iconKey.orEmpty()
        } else {
            iconKey
        }
        TaskIconPickerSheet(
            selectedIconKey = targetCurrentKey,
            taskTitleForAuto = targetTitle,
            onSelectIconKey = { pickedKey ->
                if (targetAiIdx != null) {
                    editableAiTasks = editableAiTasks.mapIndexed { idx, t ->
                        if (idx == targetAiIdx) {
                            t.copy(iconKey = pickedKey.ifBlank { TaskIconCatalog.autoDetectIconKey(t.title) })
                        } else {
                            t
                        }
                    }
                } else {
                    iconKey = pickedKey
                }
            },
            onDismiss = {
                showIconPicker = false
                iconPickerTargetAiIdx = null
            },
        )
    }

    pendingDeleteSubtaskId?.let { subId ->
        val subTitle = subtasks.firstOrNull { it.id == subId }?.title ?: "langkah ini"
        ConfirmDeleteDialog(
            title = "Hapus langkah kecil?",
            message = "Langkah \"$subTitle\" akan dihapus dari daftar.",
            onConfirm = {
                subtasks = subtasks.filterNot { it.id == subId }
                pendingDeleteSubtaskId = null
            },
            onDismiss = { pendingDeleteSubtaskId = null },
        )
    }

    pendingAiConflicts?.let { conflicts ->
        val conflictTitles = conflicts.joinToString(", ") { c ->
            "${c.importedTask.title} ↔ ${c.conflictingExistingTasks.joinToString { it.title }}"
        }
        AlertDialog(
            onDismissRequest = { pendingAiConflicts = null },
            shape = MaterialTheme.shapes.large,
            containerColor = atmo.sheetBg,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Ada Jam yang Bentrok",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${conflicts.size} jadwal baru bentrok dengan jadwal lama ($conflictTitles). Mau diapakan?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BouncyButton(
                        onClick = {
                            haptics.tapConfirm()
                            pendingAiConflicts = null
                            val msgs = conflicts.map { it.importedTask.title }
                            if (onSaveBatchWithResolution != null) {
                                onSaveBatchWithResolution(editableAiTasks, msgs, AiConflictResolution.OVERWRITE_EXISTING)
                            } else {
                                onSaveBatch(editableAiTasks, msgs)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("conflict_overwrite_button"),
                    ) {
                        Text("Timpa jadwal lama", fontWeight = FontWeight.Bold)
                    }
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            pendingAiConflicts = null
                            val msgs = conflicts.map { it.importedTask.title }
                            if (onSaveBatchWithResolution != null) {
                                onSaveBatchWithResolution(editableAiTasks, msgs, AiConflictResolution.KEEP_EXISTING_SKIP_NEW)
                            } else {
                                onSaveBatch(editableAiTasks, msgs)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("conflict_keep_old_button"),
                    ) {
                        Text("Simpan yang lama saja")
                    }
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            pendingAiConflicts = null
                            val msgs = conflicts.map { it.importedTask.title }
                            if (onSaveBatchWithResolution != null) {
                                onSaveBatchWithResolution(editableAiTasks, msgs, AiConflictResolution.KEEP_BOTH_CONFLICTING)
                            } else {
                                onSaveBatch(editableAiTasks, msgs)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("conflict_keep_both_button"),
                    ) {
                        Text("Biarkan bentrok (simpan dua-duanya)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                BouncyTextButton(onClick = { pendingAiConflicts = null }) {
                    Text("Batal")
                }
            },
            modifier = Modifier.testTag("ai_conflict_resolution_dialog"),
        )
    }

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
        onDismissRequest = { handleCloseOrCancel() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
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
                        .padding(top = 10.dp, bottom = 6.dp)
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
                .background(atmo.sheetBg),
        ) {
            // Fixed Top Header + Mode Switcher (Otomatis | Manual)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (initialTask == null) {
                            if (initialDraft != null) "Lanjutkan Draf Jadwal" else "Tambah Jadwal"
                        } else {
                            "Edit Jadwal"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    BouncyIconButton(
                        onClick = {
                            haptics.tapSoft()
                            handleCloseOrCancel()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_editor_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup modal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (initialTask == null) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_mode_switcher"),
                    ) {
                        SegmentedButton(
                            selected = isOtomatisTab,
                            onClick = {
                                haptics.tapSoft()
                                isOtomatisTab = true
                                aiError = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.testTag("editor_mode_otomatis"),
                        ) {
                            Text("Otomatis")
                        }
                        SegmentedButton(
                            selected = !isOtomatisTab,
                            onClick = {
                                haptics.tapSoft()
                                isOtomatisTab = false
                                formError = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.testTag("editor_mode_manual"),
                        ) {
                            Text("Manual")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Form Body with generous bottom padding so text never collides with sticky footer
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AnimatedContent(
                    targetState = isOtomatisTab && initialTask == null,
                    transitionSpec = {
                        fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                    },
                    label = "editor_tab_content",
                ) { showOtomatis ->
                    if (showOtomatis) {
                        // ================= OTOMATIS TAB (Zero-API AI Copy-Paste Batch Generator) =================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            val todayIso = remember { DateTimeHelper.todayIso() }
                            val promptText = remember(todayIso) {
                                AiBatchScheduleParser.buildAiPrompt(todayIso)
                            }

                            // Step 1: Copyable AI Prompt Card
                            Surface(
                                color = atmo.sheetCardBg,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, atmo.accentCobalt.copy(alpha = 0.45f)),
                                modifier = Modifier.testTag("ai_prompt_card"),
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
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                tint = atmo.accentCobalt,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = "1. Salin Prompt untuk AI",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(999.dp),
                                        ) {
                                            Text(
                                                text = todayIso,
                                                style = MonoSmallStyle,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Salin instruksi ini lalu kirim ke AI favoritmu (ChatGPT, Gemini, dll). Ceritakan rencana kegiatanmu—bisa langsung banyak sekaligus!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    ) {
                                        Text(
                                            text = promptText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(10.dp),
                                        )
                                    }

                                    BouncyButton(
                                        onClick = {
                                            haptics.tapConfirm()
                                            clipboard.setText(AnnotatedString(promptText))
                                            promptCopied = true
                                            onNotifyMessage("Prompt berhasil disalin!")
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (promptCopied) atmo.accentMint else atmo.accentCobalt,
                                            contentColor = if (promptCopied) atmo.onAccentMint else atmo.onAccentCobalt,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("copy_ai_prompt_button"),
                                    ) {
                                        Icon(
                                            imageVector = if (promptCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (promptCopied) "Prompt Tersalin! Tempel ke AI" else "Salin Prompt untuk AI",
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            // Step 2: Paste & Batch-Generate Box
                            Surface(
                                color = atmo.sheetCardBg,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                        Text(
                                            text = "2. Tempel (Paste) Hasil Teks dari AI di Sini",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (aiRawText.isNotBlank()) {
                                            BouncyTextButton(
                                                onClick = {
                                                    haptics.tapSoft()
                                                    aiRawText = ""
                                                    aiError = null
                                                },
                                            ) {
                                                Text("Bersihkan")
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = aiRawText,
                                        onValueChange = {
                                            aiRawText = it
                                            aiError = null
                                        },
                                        placeholder = {
                                            Text("Tempel hasil JSON jadwal dari AI di sini...")
                                        },
                                        minLines = 5,
                                        maxLines = 9,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shakeOnTrigger(validationShakeTrigger)
                                            .testTag("ai_paste_input"),
                                    )

                                    Text(
                                        text = "Format JSON saja ya — salin prompt di atas, kirim ke AI, lalu tempel balasan JSON-nya.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    if (liveAiPreview != null && liveAiPreview.errorMessage == null && editableAiTasks.isNotEmpty()) {
                                        Surface(
                                            color = statusPalette.greenBg,
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, statusPalette.greenAccent.copy(alpha = 0.45f)),
                                            modifier = Modifier.testTag("ai_batch_preview_badge"),
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    text = "Siap impor ${editableAiTasks.size} jadwal (bisa edit dulu di bawah).",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = statusPalette.greenFg,
                                                )
                                                if (liveAiPreview.conflictMessages.isNotEmpty()) {
                                                    Text(
                                                        text = "Perhatian: ${liveAiPreview.conflictMessages.size} jadwal beririsan jam — kamu bisa pilih aksi saat simpan.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = statusPalette.redFg,
                                                    )
                                                }
                                                editableAiTasks.forEachIndexed { idx, aiTask ->
                                                    val itemIcon = TaskIconCatalog.resolveSpec(aiTask.iconKey, aiTask.title)
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                                        shape = RoundedCornerShape(14.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("ai_preview_item_$idx"),
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            ) {
                                                                Surface(
                                                                    onClick = {
                                                                        haptics.tapSoft()
                                                                        iconPickerTargetAiIdx = idx
                                                                        showIconPicker = true
                                                                    },
                                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    modifier = Modifier
                                                                        .size(40.dp)
                                                                        .testTag("ai_preview_icon_$idx"),
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(
                                                                            imageVector = itemIcon.icon,
                                                                            contentDescription = "Ubah ikon ${aiTask.title}",
                                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                            modifier = Modifier.size(20.dp),
                                                                        )
                                                                    }
                                                                }
                                                                OutlinedTextField(
                                                                    value = aiTask.title,
                                                                    onValueChange = { newTitle ->
                                                                        editableAiTasks = editableAiTasks.mapIndexed { i, t ->
                                                                            if (i == idx) t.copy(title = newTitle) else t
                                                                        }
                                                                    },
                                                                    singleLine = true,
                                                                    label = { Text("Judul #${idx + 1}") },
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .testTag("ai_preview_title_$idx"),
                                                                )
                                                            }
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                            ) {
                                                                Text(
                                                                    text = DateTimeHelper.formatShortDate(aiTask.dateIso),
                                                                    style = MonoSmallStyle,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                )
                                                                BouncyOutlinedButton(
                                                                    onClick = {
                                                                        TimePickerDialog(
                                                                            context,
                                                                            { _, h, m ->
                                                                                val newStart = h * 60 + m
                                                                                editableAiTasks = editableAiTasks.mapIndexed { i, t ->
                                                                                    if (i == idx) {
                                                                                        val end = if (t.endMinuteOfDay <= newStart) (newStart + 60).coerceAtMost(23 * 60 + 59) else t.endMinuteOfDay
                                                                                        t.copy(startMinuteOfDay = newStart, endMinuteOfDay = end)
                                                                                    } else {
                                                                                        t
                                                                                    }
                                                                                }
                                                                            },
                                                                            aiTask.startMinuteOfDay / 60,
                                                                            aiTask.startMinuteOfDay % 60,
                                                                            true,
                                                                        ).show()
                                                                    },
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    modifier = Modifier.weight(1f),
                                                                ) {
                                                                    Text(DateTimeHelper.formatMinuteOfDay(aiTask.startMinuteOfDay), style = MonoSmallStyle)
                                                                }
                                                                BouncyOutlinedButton(
                                                                    onClick = {
                                                                        TimePickerDialog(
                                                                            context,
                                                                            { _, h, m ->
                                                                                val newEnd = (h * 60 + m).coerceAtLeast(aiTask.startMinuteOfDay + 1)
                                                                                editableAiTasks = editableAiTasks.mapIndexed { i, t ->
                                                                                    if (i == idx) t.copy(endMinuteOfDay = newEnd) else t
                                                                                }
                                                                            },
                                                                            aiTask.endMinuteOfDay / 60,
                                                                            aiTask.endMinuteOfDay % 60,
                                                                            true,
                                                                        ).show()
                                                                    },
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    modifier = Modifier.weight(1f),
                                                                ) {
                                                                    Text(DateTimeHelper.formatMinuteOfDay(aiTask.endMinuteOfDay), style = MonoSmallStyle)
                                                                }
                                                            }
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            ) {
                                                                PriorityQuadrant.entries.forEach { q ->
                                                                    BouncyFilterChip(
                                                                        selected = aiTask.quadrant == q,
                                                                        onClick = {
                                                                            haptics.tapTick()
                                                                            editableAiTasks = editableAiTasks.mapIndexed { i, t ->
                                                                                if (i == idx) t.copy(quadrant = q) else t
                                                                            }
                                                                        },
                                                                        label = { Text(q.shortCode) },
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .testTag("ai_preview_quadrant_${idx}_${q.shortCode}"),
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    aiError?.let { err ->
                                        Text(
                                            text = err,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.testTag("ai_paste_error"),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ================= MANUAL TAB =================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = {
                                    title = it
                                    formError = null
                                },
                                label = { Text("Judul Kegiatan") },
                                placeholder = { Text("Tulis nama kegiatan atau agenda") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shakeOnTrigger(validationShakeTrigger)
                                    .testTag("task_title_input"),
                            )

                            // Icon Picker Row (136 icons + Auto-Detect from title)
                            val previewIconEntry = remember(iconKey, title) {
                                TaskIconCatalog.resolveSpec(iconKey, title)
                            }
                            Surface(
                                onClick = {
                                    haptics.tapSoft()
                                    iconPickerTargetAiIdx = null
                                    showIconPicker = true
                                },
                                color = atmo.sheetCardBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("task_icon_picker_button"),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(34.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = previewIconEntry.icon,
                                                contentDescription = previewIconEntry.labelId,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (iconKey.isBlank()) {
                                                "Ikon Otomatis: ${previewIconEntry.labelId}"
                                            } else {
                                                "Ikon Dipilih: ${previewIconEntry.labelId}"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Ketuk buat ganti dari ${TaskIconCatalog.totalCount} ikon",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = "Ubah",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            // Location Name + GPS Auto Fill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = locationName,
                                    onValueChange = { locationName = it },
                                    label = { Text("Lokasi (Opsional)") },
                                    placeholder = { Text("Kantor, Kampus, Rumah...") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("task_location_input"),
                                )
                                BouncyOutlinedButton(
                                    onClick = {
                                        haptics.tapSoft()
                                        if (LocationHelper.hasLocationPermission(context)) {
                                            if (defaultLocationName.isNotBlank()) {
                                                locationName = defaultLocationName
                                                locationLat = defaultLocationLat
                                                locationLng = defaultLocationLng
                                            }
                                            onDetectLocation?.invoke()
                                        } else {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("use_gps_location_button"),
                                ) {
                                    Icon(Icons.Filled.MyLocation, contentDescription = "Pakai lokasi GPS", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("GPS")
                                }
                            }

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
                                    shape = RoundedCornerShape(16.dp),
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

                            // Real-Time Conflict Warning Banner
                            AnimatedVisibility(
                                visible = draftConflicts.isNotEmpty(),
                                enter = expandVertically(
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                                ) + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                Surface(
                                    color = statusPalette.redBg,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, statusPalette.redAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("manual_conflict_banner"),
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

                            // HIGH-VISIBILITY Pengingat (Reminder) Section — Immediately below Start/End Time!
                            Surface(
                                color = atmo.sheetCardBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (reminderEnabled) atmo.accentCobalt.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.testTag("reminder_section_card"),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.NotificationsActive,
                                                contentDescription = null,
                                                tint = if (reminderEnabled) atmo.accentCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Column {
                                                Text(
                                                    text = "Pengingat",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    text = "Kirim notifikasi sebelum acara mulai.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
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
                                            modifier = Modifier.testTag("reminder_toggle_switch"),
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = reminderEnabled,
                                        enter = expandVertically(
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                                        ) + fadeIn(),
                                        exit = shrinkVertically() + fadeOut(),
                                    ) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            listOf(0, 5, 15, 30, 60).forEach { mins ->
                                                val chipText = if (mins == 0) "Tepat waktu" else "$mins mnt sblm"
                                                BouncyFilterChip(
                                                    selected = reminderLeadMinutes == mins,
                                                    onClick = {
                                                        haptics.tapTick()
                                                        reminderLeadMinutes = mins
                                                    },
                                                    label = { Text(chipText) },
                                                    modifier = Modifier.testTag("reminder_lead_$mins"),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Prioritas K1-K4 Grid 2x2 with plain everyday Indonesian descriptions
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Prioritas",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 360.dp),
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
                                                        .defaultMinSize(minHeight = 96.dp)
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
                                                    shape = RoundedCornerShape(20.dp),
                                                    border = BorderStroke(
                                                        width = if (selected) 2.5.dp else 1.2.dp,
                                                        color = qSolidBg,
                                                    ),
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(11.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Text(
                                                                text = "${q.shortCode} • ${q.chipLabelId}",
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
                                                        Text(
                                                            text = q.subtitleId,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = animatedContentColor.copy(alpha = 0.94f),
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

                            // Catatan Opsional
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Catatan (Opsional)") },
                                placeholder = { Text("Tambahkan catatan, lokasi, atau perlengkapan (opsional)...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_notes_input"),
                            )

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
                                        placeholder = {
                                            Text(
                                                if (isSubtasksTodoMode) {
                                                    "Tulis item daftar tugas..."
                                                } else {
                                                    "Tulis langkah kecil kegiatan..."
                                                },
                                            )
                                        },
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
                                        shape = RoundedCornerShape(16.dp),
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
                                                    pendingDeleteSubtaskId = sub.id
                                                },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("delete_subtask_${sub.id}"),
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

                            formError?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            // Sticky Solid Opaque Bottom Action Bar
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                color = atmo.sheetBg,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            handleCloseOrCancel()
                        },
                        modifier = Modifier
                            .weight(0.38f)
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("cancel_editor_button"),
                    ) {
                        Text("Batal")
                    }

                    if (isOtomatisTab && initialTask == null) {
                        BouncyButton(
                            onClick = {
                                val parsed = AiBatchScheduleParser.parse(
                                    rawText = aiRawText,
                                    defaultDateIso = dateIso,
                                    existingTasks = existingTasks,
                                )
                                val tasksToSave = if (editableAiTasks.isNotEmpty() && parsed.errorMessage == null) {
                                    editableAiTasks
                                } else {
                                    parsed.tasks
                                }
                                if (parsed.errorMessage != null || tasksToSave.isEmpty()) {
                                    haptics.tapWarning()
                                    validationShakeTrigger += 1
                                    aiError = parsed.errorMessage ?: "Gagal membaca jadwal dari teks."
                                } else {
                                    val conflicts = AiBatchScheduleParser.detectDetailedConflicts(tasksToSave, existingTasks)
                                    if (conflicts.isNotEmpty()) {
                                        haptics.tapWarning()
                                        pendingAiConflicts = conflicts
                                    } else {
                                        haptics.tapConfirm()
                                        if (onSaveBatchWithResolution != null) {
                                            onSaveBatchWithResolution(
                                                tasksToSave,
                                                parsed.conflictMessages,
                                                AiConflictResolution.KEEP_BOTH_CONFLICTING,
                                            )
                                        } else {
                                            onSaveBatch(tasksToSave, parsed.conflictMessages)
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = atmo.accentCobalt,
                                contentColor = atmo.onAccentCobalt,
                            ),
                            modifier = Modifier
                                .weight(0.62f)
                                .height(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("generate_ai_schedule_button"),
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buat Jadwal Otomatis", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
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
                                                notes = notes.trim(),
                                                iconKey = iconKey.ifBlank { TaskIconCatalog.autoDetectIconKey(trimmed) },
                                                locationName = locationName.trim(),
                                                locationLat = locationLat,
                                                locationLng = locationLng,
                                            ),
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(0.62f)
                                .height(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("save_task_button"),
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}
