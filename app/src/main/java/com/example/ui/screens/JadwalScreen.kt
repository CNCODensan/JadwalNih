package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DateTimeHelper
import com.example.domain.ForeshadowOccurrence
import com.example.domain.HolidayThemeType
import com.example.domain.PhotoRecap
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.TaskDraft
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.domain.WorshipScheduleItem
import com.example.ui.JadwalNihUiState
import com.example.ui.components.BirthdayTodayMiniCard
import com.example.ui.components.BouncyButton
import com.example.ui.components.BouncyFilterChip
import com.example.ui.components.BouncyIconButton
import com.example.ui.components.BouncyOutlinedButton
import com.example.ui.components.BouncyTextButton
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ForeshadowActionSheet
import com.example.ui.components.ForeshadowGhostCard
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.HolidayGreetingBanner
import com.example.ui.components.RingkasanCepatCard
import com.example.ui.components.ScheduleTaskCard
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.V1WelcomeCard
import com.example.ui.components.WorshipScheduleCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.colorsFor
import com.example.ui.components.tapConfirm
import com.example.ui.components.tapSoft
import com.example.ui.components.tapTick
import com.example.ui.theme.HolidayScheduleWatermarkOverlay
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JadwalScreen(
    state: JadwalNihUiState,
    onSelectScope: (ScheduleScope) -> Unit,
    onSelectDate: (String) -> Unit,
    onStepDate: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectStatusFilter: (TaskStatus?) -> Unit,
    onToggleComplete: (ScheduleTask) -> Unit,
    onToggleSubTask: (ScheduleTask, String) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onExportCalendar: (ScheduleTask) -> Unit,
    onEditTask: (ScheduleTask) -> Unit,
    onDeleteTask: (ScheduleTask) -> Unit,
    onQuickGlanceClick: () -> Unit,
    onResumeDraft: (TaskDraft) -> Unit,
    onDiscardDraft: () -> Unit,
    onConfirmAttendance: (ScheduleTask, Boolean) -> Unit,
    onOverrideAttended: (ScheduleTask) -> Unit,
    onRescheduleYesterday: (Set<Long>) -> Unit = {},
    onSkipYesterday: (Set<Long>) -> Unit = {},
    onOpenFocusTab: () -> Unit,
    onOpenRecapTab: () -> Unit,
    onOpenMatrixTab: () -> Unit,
    onOpenAnalyticsTab: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteRecurringSeries: (ScheduleTask) -> Unit = { onDeleteTask(it) },
    onDismissWelcomeCard: () -> Unit = {},
    onToggleWorshipComplete: (WorshipScheduleItem) -> Unit = {},
    onActivateForeshadowEarly: (ForeshadowOccurrence) -> Unit = {},
    onSkipForeshadow: (ForeshadowOccurrence) -> Unit = {},
    onOpenWorshipHistory: () -> Unit = {},
    onReplayHolidayMotion: () -> Unit = {},
    onOpenBirthdayCelebration: () -> Unit = {},
) {
    val todayIso = state.currentDateIso.ifBlank { DateTimeHelper.todayIso() }
    val statusPalette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val haptics = LocalHapticFeedback.current

    var pendingConfirmDiscardDraft by remember { mutableStateOf(false) }
    var selectedForeshadow by remember { mutableStateOf<ForeshadowOccurrence?>(null) }

    if (pendingConfirmDiscardDraft) {
        ConfirmDeleteDialog(
            title = "Hapus Draf Jadwal?",
            message = "Draf yang belum disimpan bakal dihapus permanen. Yakin mau lanjut hapus?",
            confirmLabel = "Hapus Draf",
            onConfirm = {
                pendingConfirmDiscardDraft = false
                onDiscardDraft()
            },
            onDismiss = { pendingConfirmDiscardDraft = false },
        )
    }

    selectedForeshadow?.let { occurrence ->
        ForeshadowActionSheet(
            occurrence = occurrence,
            onActivateEarly = {
                selectedForeshadow = null
                onActivateForeshadowEarly(occurrence)
            },
            onSkipWeek = {
                selectedForeshadow = null
                onSkipForeshadow(occurrence)
            },
            onDismiss = { selectedForeshadow = null },
        )
    }

    val weekDaysSet = remember(state.selectedDateIso) {
        DateTimeHelper.weekDaysFor(state.selectedDateIso).toSet()
    }
    val visibleForeshadows = remember(
        state.foreshadowOccurrences,
        state.selectedScope,
        state.selectedDateIso,
        weekDaysSet,
        state.filteredScheduleTasks,
    ) {
        val existingSignatures = state.filteredScheduleTasks.map {
            "${it.task.dateIso}|${it.task.title.trim().lowercase()}|${it.task.startMinuteOfDay}"
        }.toSet()
        state.foreshadowOccurrences.filter { occ ->
            val inScope = when (state.selectedScope) {
                ScheduleScope.DAILY -> occ.occurrenceDateIso == state.selectedDateIso
                ScheduleScope.WEEKLY -> occ.occurrenceDateIso in weekDaysSet
                ScheduleScope.MONTHLY -> occ.occurrenceDateIso.startsWith(state.selectedDateIso.take(7))
            }
            val sig = "${occ.occurrenceDateIso}|${occ.parentTask.title.trim().lowercase()}|${occ.parentTask.startMinuteOfDay}"
            inScope && sig !in existingSignatures
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HolidayScheduleWatermarkOverlay(themeType = state.activeHolidayTheme)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spec.screenHorizontalPadding,
                end = spec.screenHorizontalPadding,
                top = 4.dp,
                bottom = spec.fabContentClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        // 1. Info-only Quick Glance Hero Card (tappable to jump to Analitik / Matriks in Normal Mode)
        item(key = "quick_glance") {
            RingkasanCepatCard(
                summary = state.quickGlance,
                greeting = state.greeting,
                tapTarget = state.quickGlanceTarget,
                isSimpleMode = state.isSimpleMode,
                onCardClick = onQuickGlanceClick,
            )
        }

        // 1a. Birthday Celebration Mini Card (shown when today matches user's birthday)
        if (state.isUserBirthdayToday) {
            item(key = "birthday_today_mini") {
                BirthdayTodayMiniCard(
                    userName = state.userName,
                    stats = state.personalBirthdayStats,
                    onOpenCelebration = onOpenBirthdayCelebration,
                )
            }
        }

        // 1b. v1.0 Welcome Card (shown once for new onboarded users, easily dismissible)
        if (state.hasCompletedOnboarding && !state.hasSeenV1WelcomeCard) {
            item(key = "v1_welcome_card") {
                V1WelcomeCard(onDismiss = onDismissWelcomeCard)
            }
        }

        // 1c. Public Holiday Greeting Banner (on Hari H or when theme preview active)
        val activeHoliday = state.todayHoliday ?: state.selectedDateHoliday
        if (state.activeHolidayTheme != HolidayThemeType.NONE || activeHoliday != null) {
            item(key = "holiday_greeting_banner") {
                HolidayGreetingBanner(
                    holiday = activeHoliday,
                    themeType = state.activeHolidayTheme,
                    onReplayMotion = onReplayHolidayMotion,
                )
            }
        }

        // 2. Draft Recovery Banner (if user closed TaskEditorSheet with unsaved draft)
        val draft = state.taskDraft?.takeIf { it.hasMeaningfulContent() }
        if (draft != null) {
            item(key = "draft_recovery_banner") {
                FrostedGlassCard(
                    cornerRadius = 22.dp,
                    borderColor = atmo.accentCobalt.copy(alpha = 0.55f),
                    modifier = Modifier.testTag("draft_recovery_banner"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.EditNote,
                                        contentDescription = "Draf jadwal belum disimpan",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Draf jadwal belum disimpan. Mau lanjut edit atau hapus draf?",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val previewText = when {
                                    draft.title.isNotBlank() -> draft.title
                                    draft.aiRawText.isNotBlank() -> draft.aiRawText.trim()
                                    draft.notes.isNotBlank() -> draft.notes.trim()
                                    else -> "${DateTimeHelper.formatShortDate(draft.dateIso)} • ${
                                        DateTimeHelper.formatTimeRange(draft.startMinuteOfDay, draft.endMinuteOfDay)
                                    }"
                                }
                                Text(
                                    text = previewText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapSoft()
                                    pendingConfirmDiscardDraft = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("discard_draft_button"),
                            ) {
                                Text("Hapus Draf")
                            }
                            BouncyButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onResumeDraft(draft)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = atmo.accentCobalt,
                                    contentColor = atmo.onAccentCobalt,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("resume_draft_button"),
                            ) {
                                Text("Lanjut Edit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive Attendance Check-In Card OR Recent Missed Event Override Card
        val checkInItem = state.checkInPromptTask
        val missedItem = state.recentMissedTaskForOverride
        if (checkInItem != null) {
            item(key = "attendance_checkin_card") {
                val task = checkInItem.task
                FrostedGlassCard(
                    cornerRadius = 22.dp,
                    borderColor = atmo.accentMint.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("checkin_prompt_card"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                color = statusPalette.greenBg,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = "Konfirmasi kehadiran",
                                        tint = statusPalette.greenFg,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Konfirmasi Kehadiran",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = statusPalette.greenFg,
                                )
                                Text(
                                    text = "Apakah kamu sedang mengikuti \"${task.title}\"?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay),
                                    style = MonoSmallStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onConfirmAttendance(task, false)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.42f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("checkin_confirm_no"),
                            ) {
                                Text("Belum")
                            }
                            BouncyButton(
                                onClick = {
                                    haptics.tapConfirm()
                                    onConfirmAttendance(task, true)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = atmo.accentMint,
                                    contentColor = atmo.onAccentMint,
                                ),
                                modifier = Modifier
                                    .weight(0.58f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("checkin_confirm_yes"),
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ya, Sedang Hadir")
                            }
                        }
                    }
                }
            }
        } else if (missedItem != null) {
            item(key = "missed_override_card") {
                val task = missedItem.task
                FrostedGlassCard(
                    cornerRadius = 22.dp,
                    borderColor = statusPalette.darkAccent.copy(alpha = 0.65f),
                    modifier = Modifier.testTag("missed_override_card"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = statusPalette.darkBg,
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Jadwal terlewat",
                                    tint = statusPalette.darkFg,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "\"${task.title}\" Terlewat",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Tadi sempat hadir tapi lupa konfirmasi?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BouncyButton(
                            onClick = {
                                haptics.tapConfirm()
                                onOverrideAttended(task)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = atmo.accentMint,
                                contentColor = atmo.onAccentMint,
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("override_missed_attended"),
                        ) {
                            Text("Ternyata Hadir", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 3b. Smart Reschedule Prompt for yesterday's unfinished/missed tasks
        if (state.yesterdayMissedTasks.isNotEmpty()) {
            item(key = "yesterday_reschedule_card") {
                val allMissedIds = remember(state.yesterdayMissedTasks) {
                    state.yesterdayMissedTasks.map { it.task.id }.toSet()
                }
                var selectedIds by remember(allMissedIds) {
                    mutableStateOf(allMissedIds)
                }
                val count = state.yesterdayMissedTasks.size
                val allSelected = selectedIds.size == allMissedIds.size

                FrostedGlassCard(
                    cornerRadius = 22.dp,
                    borderColor = atmo.accentAmber.copy(alpha = 0.65f),
                    modifier = Modifier.testTag("yesterday_reschedule_card"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                color = atmo.accentAmber.copy(alpha = 0.18f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = "Jadwal kemarin belum selesai",
                                        tint = atmo.accentOrange,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Jadwal Kemarin Belum Selesai",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Ada $count kegiatan kemarin yang belum tuntas. Pilih yang mau dipindah ke hari ini atau lewati.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (count > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                BouncyTextButton(
                                    onClick = {
                                        haptics.tapTick()
                                        selectedIds = if (allSelected) emptySet() else allMissedIds
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("yesterday_reschedule_select_all"),
                                ) {
                                    Text(
                                        text = if (allSelected) "Kosongkan Pilihan" else "Pilih Semua ($count)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.yesterdayMissedTasks.forEach { missed ->
                                val task = missed.task
                                val isChecked = task.id in selectedIds
                                Surface(
                                    color = if (isChecked) {
                                        atmo.accentAmber.copy(alpha = if (atmo.isDark) 0.15f else 0.10f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isChecked) {
                                            atmo.accentAmber.copy(alpha = 0.55f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        },
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            haptics.tapTick()
                                            selectedIds = if (isChecked) {
                                                selectedIds - task.id
                                            } else {
                                                selectedIds + task.id
                                            }
                                        }
                                        .minimumInteractiveComponentSize()
                                        .testTag("yesterday_reschedule_item_${task.id}"),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                haptics.tapTick()
                                                selectedIds = if (checked) {
                                                    selectedIds + task.id
                                                } else {
                                                    selectedIds - task.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = atmo.accentAmber,
                                                checkmarkColor = atmo.onAccentAmber,
                                            ),
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay),
                                                style = MonoSmallStyle,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onSkipYesterday(allMissedIds)
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("yesterday_reschedule_skip_button"),
                            ) {
                                Text("Lewati", maxLines = 1)
                            }
                            val moveLabel = when {
                                selectedIds.isEmpty() -> "Pilih Jadwal"
                                count == 1 -> "Pindah ke Hari Ini"
                                selectedIds.size == count -> "Pindah Semua ($count)"
                                else -> "Pindah Terpilih (${selectedIds.size})"
                            }
                            BouncyButton(
                                onClick = {
                                    haptics.tapConfirm()
                                    onRescheduleYesterday(selectedIds)
                                },
                                enabled = selectedIds.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = atmo.accentAmber,
                                    contentColor = atmo.onAccentAmber,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("yesterday_reschedule_button"),
                            ) {
                                Text(moveLabel, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // 3c. Worship / Prayer Schedule Card (strictly gated by active window in ONLY_ACTIVE_WINDOW mode)
        if (state.prayerReminderEnabled && state.todayWorshipItems.isNotEmpty()) {
            item(key = "worship_schedule_card") {
                WorshipScheduleCard(
                    religion = state.religion,
                    items = state.todayWorshipItems,
                    nextItem = state.nextWorshipItem,
                    countdownLabel = state.nextWorshipCountdown,
                    locationName = state.currentLocationName,
                    onToggleComplete = onToggleWorshipComplete,
                    visibilityMode = state.prayerCardVisibilityMode,
                    activeWindow = state.activeWorshipWindow,
                    onOpenHistory = onOpenWorshipHistory,
                )
            }
        }

        // 4. Bento Grid Shortcut Cards below Quick Glance (compact, Matriks & Analitik hidden in Simple Mode)
        item(key = "bento_shortcuts") {
            val todayPhotoCount = state.photoRecaps[todayIso]?.imagePaths?.size ?: 0
            val focusText = if (state.focusTimer.hasActiveSession) {
                DateTimeHelper.formatCountdown(state.focusTimer.remainingSeconds)
            } else {
                "${state.focusTimer.plannedMinutes} mnt siap"
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StaggeredEntrance(index = 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BentoShortcutTile(
                            title = "Fokus",
                            subtitle = focusText,
                            icon = Icons.Filled.Timer,
                            containerColor = atmo.accentOrange,
                            contentColor = atmo.onAccentOrange,
                            onClick = onOpenFocusTab,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bento_fokus"),
                        )
                        BentoShortcutTile(
                            title = "Rekap Foto",
                            subtitle = "$todayPhotoCount/4 hari ini",
                            icon = Icons.Filled.CameraAlt,
                            containerColor = atmo.accentMint,
                            contentColor = atmo.onAccentMint,
                            onClick = onOpenRecapTab,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bento_rekap"),
                        )
                    }
                }
                if (!state.isSimpleMode) {
                    StaggeredEntrance(index = 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BentoShortcutTile(
                                title = "Matriks",
                                subtitle = "Timeline & Prioritas",
                                icon = Icons.Filled.GridView,
                                containerColor = atmo.accentCoral,
                                contentColor = atmo.onAccentCoral,
                                onClick = onOpenMatrixTab,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bento_matriks"),
                            )
                            BentoShortcutTile(
                                title = "Analitik",
                                subtitle = "${state.analytics.completionRatePercent}% tuntas",
                                icon = Icons.Filled.BarChart,
                                containerColor = atmo.accentAmber,
                                contentColor = atmo.onAccentAmber,
                                onClick = onOpenAnalyticsTab,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bento_analitik"),
                            )
                        }
                    }
                }
            }
        }

        // 5. Scope Segmented Control
        item(key = "scope_selector") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_scope_segmented"),
            ) {
                ScheduleScope.entries.forEachIndexed { index, scope ->
                    val segInteraction = remember { MutableInteractionSource() }
                    SegmentedButton(
                        selected = state.selectedScope == scope,
                        onClick = {
                            haptics.tapSoft()
                            onSelectScope(scope)
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ScheduleScope.entries.size,
                        ),
                        interactionSource = segInteraction,
                        modifier = Modifier
                            .bouncyPress(segInteraction, pressedScale = 0.93f)
                            .testTag("scope_tab_${scope.name.lowercase()}"),
                    ) {
                        Text(scope.label)
                    }
                }
            }
        }

        item(key = "date_navigator") {
            FrostedGlassCard(cornerRadius = 22.dp) {
                Column(
                    modifier = Modifier
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                        .padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                                BouncyTextButton(
                                    onClick = {
                                        haptics.tapSoft()
                                        onJumpToToday()
                                    },
                                ) {
                                    Text("Ke Hari Ini", style = MaterialTheme.typography.labelSmall)
                                }
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

        item(key = "daily_load_summary") {
            FrostedGlassCard(cornerRadius = 18.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Beban waktu harian",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        val loadText = if (state.selectedDateLoadMinutes <= 0) {
                            "Kosong"
                        } else {
                            DateTimeHelper.formatDuration(state.selectedDateLoadMinutes)
                        }
                        Text(
                            text = "Beban ${DateTimeHelper.formatShortDate(state.selectedDateIso)}: $loadText",
                            style = MonoSmallStyle.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (state.selectedDateConflictCount > 0) {
                        Surface(
                            color = statusPalette.redBg,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, statusPalette.redAccent.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = "Bentrok jam",
                                    tint = statusPalette.redFg,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "${state.selectedDateConflictCount} bentrok",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusPalette.redFg,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (state.selectedDateLoadMinutes > 0) "Jam aman" else "Santai",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item(key = "status_filters") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.prayerReminderEnabled && state.todayWorshipItems.isNotEmpty()) {
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            onOpenWorshipHistory()
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("jadwal_open_worship_history_chip"),
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ibadah 7 Hari", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatusFilterRow(
                        selectedStatus = state.statusFilter,
                        onSelectStatus = onSelectStatusFilter,
                    )
                }
            }
        }

        if (state.filteredScheduleTasks.isEmpty() && visibleForeshadows.isEmpty()) {
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
                            color = MaterialTheme.colorScheme.onSurface,
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
                    onDeleteRecurringSeries = onDeleteRecurringSeries,
                    conflictInfo = state.conflictsByTaskId[item.task.id],
                    onToggleSubTask = onToggleSubTask,
                    onOverrideAttended = onOverrideAttended,
                    todayIso = todayIso,
                    nowMinuteOfDay = state.currentMinuteOfDay,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (visibleForeshadows.isNotEmpty()) {
            item(key = "foreshadow_section_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Bayangan Jadwal Berulang (4 Minggu Ke Depan)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Otomatis aktif saat minggu tiba. Ketuk untuk edit lebih awal atau lewati.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(
                items = visibleForeshadows,
                key = { "foreshadow_${it.parentTask.id}_${it.occurrenceDateIso}" },
            ) { occ ->
                ForeshadowGhostCard(
                    occurrence = occ,
                    onClick = { selectedForeshadow = occ },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        }
    }
}

@Composable
private fun BentoShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = {
            haptics.tapSoft()
            onClick()
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .bouncyPress(interaction, pressedScale = 0.94f)
            .minimumInteractiveComponentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = contentColor.copy(alpha = 0.16f),
                shape = CircleShape,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    val haptics = LocalHapticFeedback.current

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

            val cellBg by animateColorAsState(
                targetValue = when {
                    isSelected -> atmo.accentCobalt
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    else -> atmo.cardAltBg.copy(alpha = 0.72f)
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "week_cell_bg_$dayIso",
            )
            val selectedScale by animateFloatAsState(
                targetValue = if (isSelected) 1.03f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "week_cell_scale_$dayIso",
            )

            val dayInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = selectedScale
                        scaleY = selectedScale
                    }
                    .bouncyPress(dayInteraction, pressedScale = 0.90f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cellBg)
                    .clickable(
                        interactionSource = dayInteraction,
                        indication = null,
                    ) {
                        haptics.tapSoft()
                        onSelectDate(dayIso)
                    }
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
                        color = if (isSelected) atmo.onAccentCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = DateTimeHelper.dayOfMonthNumber(dayIso).toString(),
                        style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) atmo.onAccentCobalt else MaterialTheme.colorScheme.onSurface,
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
                                    .background(if (isSelected) atmo.accentAmber else atmo.accentMint),
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
    val haptics = LocalHapticFeedback.current
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

                        val cellBg by animateColorAsState(
                            targetValue = when {
                                isSelected -> atmo.accentCobalt
                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                else -> atmo.cardAltBg.copy(alpha = 0.65f)
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "month_cell_bg_${cell.dateIso}",
                        )
                        val cellScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.04f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            label = "month_cell_scale_${cell.dateIso}",
                        )

                        val cellInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .graphicsLayer {
                                    scaleX = cellScale
                                    scaleY = cellScale
                                }
                                .bouncyPress(cellInteraction, pressedScale = 0.90f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(cellBg)
                                .clickable(
                                    interactionSource = cellInteraction,
                                    indication = null,
                                ) {
                                    haptics.tapSoft()
                                    onSelectDate(cell.dateIso)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = cell.dayOfMonth.toString(),
                                    style = MonoSmallStyle,
                                    color = if (isSelected) atmo.onAccentCobalt else MaterialTheme.colorScheme.onSurface,
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
                                                .background(if (isSelected) atmo.accentAmber else atmo.accentMint),
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
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BouncyFilterChip(
            selected = selectedStatus == null,
            onClick = {
                haptics.tapSoft()
                onSelectStatus(null)
            },
            label = { Text("Semua") },
        )
        TaskStatus.entries.forEach { status ->
            val colors = palette.colorsFor(status)
            BouncyFilterChip(
                selected = selectedStatus == status,
                onClick = {
                    haptics.tapSoft()
                    onSelectStatus(status)
                },
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
