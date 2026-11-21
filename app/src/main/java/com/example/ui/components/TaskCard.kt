package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AttendanceStatus
import com.example.domain.ConflictInfo
import com.example.domain.DateTimeHelper
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleTask
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleTaskCard(
    item: TaskWithStatus,
    onToggleComplete: (ScheduleTask) -> Unit,
    onMoveToTomorrow: (ScheduleTask) -> Unit,
    onExportCalendar: (ScheduleTask) -> Unit,
    onEdit: (ScheduleTask) -> Unit,
    onDelete: (ScheduleTask) -> Unit,
    modifier: Modifier = Modifier,
    conflictInfo: ConflictInfo? = null,
    onToggleSubTask: (ScheduleTask, String) -> Unit = { _, _ -> },
    onOverrideAttended: (ScheduleTask) -> Unit = {},
) {
    val task = item.task
    val palette = LocalStatusPalette.current
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val statusColors = palette.colorsFor(item.status)
    var subtasksExpanded by remember(task.id, task.isSubtasksTodoMode) {
        mutableStateOf(task.isSubtasksTodoMode)
    }

    val doneSubCount = task.subtasks.count { it.isDone }
    val totalSubCount = task.subtasks.size
    val subFraction by animateFloatAsState(
        targetValue = if (totalSubCount == 0) 0f else doneSubCount.toFloat() / totalSubCount.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "subtask_progress_${task.id}",
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.78f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "task_card_alpha_${task.id}",
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = statusColors.accent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "task_accent_${task.id}",
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = (if (conflictInfo != null) palette.redAccent else statusColors.accent)
            .copy(alpha = if (atmo.isDark) 0.55f else 0.42f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "task_border_${task.id}",
    )
    val completionPopScale = remember(task.id) { Animatable(1f) }
    var wasCompleted by remember(task.id) { mutableStateOf(task.isCompleted) }
    LaunchedEffect(task.isCompleted) {
        if (!wasCompleted && task.isCompleted) {
            completionPopScale.animateTo(
                targetValue = 1.028f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
            completionPopScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        }
        wasCompleted = task.isCompleted
    }

    FrostedGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardAlpha
                scaleX = completionPopScale.value
                scaleY = completionPopScale.value
            }
            .testTag("task_item_card_${task.id}"),
        cornerRadius = 18.dp,
        borderColor = animatedBorderColor,
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
                    .background(animatedAccentColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                    .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = {
                            if (!task.isCompleted) haptics.tapConfirm() else haptics.tapSoft()
                            onToggleComplete(task)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = palette.greenAccent,
                            uncheckedColor = statusColors.accent,
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("task_complete_checkbox_${task.id}"),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Waktu kegiatan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${DateTimeHelper.formatShortDate(task.dateIso)} • ${
                                    DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay)
                                }",
                                style = MonoSmallStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = DateTimeHelper.formatDuration(task.estimatedMinutes),
                                    style = MonoSmallStyle,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatusBadge(status = item.status)
                            QuadrantChip(quadrant = task.quadrant)
                            if (!task.isCompleted && task.attendanceStatus == AttendanceStatus.CONFIRMED_ATTENDING) {
                                Surface(
                                    color = palette.greenBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, palette.greenAccent.copy(alpha = 0.45f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.HowToReg,
                                            contentDescription = "Sedang hadir",
                                            tint = palette.greenFg,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = "Sedang Ikut",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.greenFg,
                                        )
                                    }
                                }
                            }
                            if (conflictInfo != null) {
                                val pulseTransition = rememberInfiniteTransition(label = "conflict_pulse_${task.id}")
                                val pulseAlpha by pulseTransition.animateFloat(
                                    initialValue = 0.72f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(900),
                                        repeatMode = RepeatMode.Reverse,
                                    ),
                                    label = "conflict_alpha",
                                )
                                Surface(
                                    color = palette.redBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, palette.redAccent.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .graphicsLayer { alpha = pulseAlpha }
                                        .testTag("task_conflict_badge_${task.id}"),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = conflictInfo.summaryText,
                                            tint = palette.redFg,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = "Bentrok",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.redFg,
                                        )
                                    }
                                }
                            }
                            if (task.recurrence != RecurrenceRule.NONE) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Repeat,
                                            contentDescription = "Tugas berulang",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = task.recurrence.labelId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }
                            if (task.reminderEnabled) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Pengingat aktif",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.CenterVertically),
                                )
                            }
                        }

                        if (conflictInfo != null) {
                            Text(
                                text = conflictInfo.summaryText,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.redFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Missed / overdue event quick attendance override button
                        if (item.status == TaskStatus.OVERDUE_DARK && !task.isCompleted) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapConfirm()
                                    onOverrideAttended(task)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, palette.greenAccent.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.greenBg.copy(alpha = 0.45f),
                                    contentColor = palette.greenFg,
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("task_override_attended_${task.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Ternyata Hadir",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                // Expandable Mini Checklist / To-Do List Sub-tasks
                if (task.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val subToggleInteraction = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .bouncyPress(subToggleInteraction, pressedScale = 0.97f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = subToggleInteraction,
                                indication = null,
                            ) {
                                haptics.tapSoft()
                                subtasksExpanded = !subtasksExpanded
                            }
                            .testTag("task_subtasks_toggle_${task.id}"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Checklist,
                                        contentDescription = "Daftar langkah kecil",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    AnimatedContent(
                                        targetState = if (task.isSubtasksTodoMode) {
                                            "To-Do List $doneSubCount/$totalSubCount"
                                        } else {
                                            "Langkah $doneSubCount/$totalSubCount"
                                        },
                                        transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(140))) },
                                        label = "subtask_count_${task.id}",
                                    ) { countLabel ->
                                        Text(
                                            text = countLabel,
                                            style = MonoSmallStyle.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                                val chevronRotation by animateFloatAsState(
                                    targetValue = if (subtasksExpanded) 180f else 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                    label = "subtasks_chevron_${task.id}",
                                )
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = if (subtasksExpanded) "Tutup langkah" else "Lihat langkah",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = chevronRotation },
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                            ) {
                                if (subFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(subFraction.coerceIn(0f, 1f))
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(atmo.accentMint),
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = subtasksExpanded,
                                enter = expandVertically(
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                ) + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    task.subtasks.forEach { sub ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (!sub.isDone) haptics.tapConfirm() else haptics.tapSoft()
                                                    onToggleSubTask(task, sub.id)
                                                }
                                                .padding(vertical = 2.dp)
                                                .testTag("subtask_row_${task.id}_${sub.id}"),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Checkbox(
                                                checked = sub.isDone,
                                                onCheckedChange = {
                                                    if (!sub.isDone) haptics.tapConfirm() else haptics.tapSoft()
                                                    onToggleSubTask(task, sub.id)
                                                },
                                                modifier = Modifier.minimumInteractiveComponentSize(),
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

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        BouncyTextButton(
                            onClick = {
                                haptics.tapSoft()
                                onMoveToTomorrow(task)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("task_move_tomorrow_${task.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Pindah ke Besok",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Besok",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        BouncyTextButton(
                            onClick = {
                                haptics.tapSoft()
                                onExportCalendar(task)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("task_export_calendar_${task.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Ekspor ke Kalender Perangkat",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kalender",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    Row {
                        BouncyIconButton(
                            onClick = {
                                haptics.tapSoft()
                                onEdit(task)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("task_edit_${task.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit jadwal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        BouncyIconButton(
                            onClick = {
                                haptics.tapSoft()
                                onDelete(task)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("task_delete_${task.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Hapus jadwal",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
