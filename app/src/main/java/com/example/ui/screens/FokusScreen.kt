package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.DateTimeHelper
import com.example.domain.ScheduleTask
import com.example.domain.TaskIconCatalog
import com.example.domain.WorshipScheduleItem
import com.example.ui.JadwalNihUiState
import com.example.ui.components.AtmosphericBackground
import com.example.ui.components.BouncyButton
import com.example.ui.components.BouncyFilledTonalButton
import com.example.ui.components.BouncyIconButton
import com.example.ui.components.BouncyOutlinedButton
import com.example.ui.components.BouncyTextButton
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.tapConfirm
import com.example.ui.components.tapSoft
import com.example.ui.components.tapTick
import com.example.ui.components.tapWarning
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.MonoSmallStyle

@Composable
fun FokusScreen(
    state: JadwalNihUiState,
    onMinutesInputChange: (String) -> Unit,
    onStepMinutes: (Int) -> Unit,
    onSelectTask: (ScheduleTask?) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onStopAndSave: () -> Unit,
    onToggleDistractionFree: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDismissWorshipPrompt: () -> Unit = {},
    onMarkWorshipDone: (WorshipScheduleItem) -> Unit = {},
) {
    val timer = state.focusTimer
    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val haptics = LocalHapticFeedback.current
    var taskMenuExpanded by remember { mutableStateOf(false) }
    var invalidInputShake by remember { mutableIntStateOf(0) }
    LaunchedEffect(timer.validationError) {
        if (timer.validationError != null) {
            invalidInputShake += 1
            haptics.tapWarning()
        }
    }
    val activeTasks = remember(state.allTasksWithStatus) {
        state.allTasksWithStatus.filterNot { it.task.isCompleted }.map { it.task }
    }
    val linkedTask = remember(timer.linkedTaskId, state.allTasksWithStatus) {
        timer.linkedTaskId?.let { id ->
            state.allTasksWithStatus.firstOrNull { it.task.id == id }?.task
        }
    }

    if (timer.isDistractionFreeMode) {
        DistractionFreeFocusDialog(
            state = state,
            linkedTask = linkedTask,
            onPause = onPause,
            onResume = onResume,
            onStopAndSave = onStopAndSave,
            onExitDistractionFree = { onToggleDistractionFree(false) },
            onDismissWorshipPrompt = onDismissWorshipPrompt,
            onMarkWorshipDone = onMarkWorshipDone,
        )
    }

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
        item(key = "focus_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Timer Fokus",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Atur durasi bebas 1–240 menit, tetap fokus pelan-pelan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.focusGentleWorshipPrompt?.let { promptItem ->
            item(key = "gentle_worship_prompt") {
                AnimatedVisibility(visible = true) {
                    GentleFocusWorshipBanner(
                        item = promptItem,
                        onMarkDone = { onMarkWorshipDone(promptItem) },
                        onDismiss = onDismissWorshipPrompt,
                    )
                }
            }
        }

        item(key = "timer_card") {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
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
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val totalSecs = (timer.plannedMinutes * 60).coerceAtLeast(1)
                    val targetProgress = (timer.remainingSeconds.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "focus_ring_progress",
                    )
                    val ringAccent by animateColorAsState(
                        targetValue = when {
                            timer.isRunning -> atmo.accentOrange
                            timer.isPaused -> atmo.accentAmber
                            else -> atmo.accentOrange
                        },
                        animationSpec = tween(durationMillis = 320),
                        label = "focus_ring_accent",
                    )
                    val ringBreathTransition = rememberInfiniteTransition(label = "focus_ring_breath")
                    val breathScale by ringBreathTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.035f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1600),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "focus_breath_scale",
                    )
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)

                    Box(
                        modifier = Modifier.size(216.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val activeBreath = if (timer.isRunning) breathScale else 1f
                            drawCircle(
                                color = ringAccent.copy(alpha = if (timer.isRunning) 0.14f else 0.08f),
                                radius = (size.minDimension / 2f) * activeBreath,
                            )
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth),
                            )
                            drawArc(
                                color = ringAccent,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = DateTimeHelper.formatCountdown(timer.remainingSeconds),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("focus_countdown_text"),
                            )
                            AnimatedContent(
                                targetState = when {
                                    timer.isRunning -> "Sedang fokus..."
                                    timer.isPaused -> "Dijeda"
                                    else -> "Target ${timer.plannedMinutes} menit"
                                },
                                transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(140))) },
                                label = "focus_status_text",
                            ) { statusLabel ->
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ringAccent,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shakeOnTrigger(invalidInputShake),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Durasi (Menit)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapTick()
                                    onStepMinutes(-5)
                                },
                                enabled = !timer.hasActiveSession,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Text("-5")
                            }
                            BouncyIconButton(
                                onClick = {
                                    haptics.tapTick()
                                    onStepMinutes(-1)
                                },
                                enabled = !timer.hasActiveSession,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Kurangi 1 menit")
                            }
                            OutlinedTextField(
                                value = timer.customMinutesInput,
                                onValueChange = onMinutesInputChange,
                                enabled = !timer.hasActiveSession,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MonoSmallStyle.copy(
                                    textAlign = TextAlign.Center,
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_custom_minutes_input"),
                            )
                            BouncyIconButton(
                                onClick = {
                                    haptics.tapTick()
                                    onStepMinutes(1)
                                },
                                enabled = !timer.hasActiveSession,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Tambah 1 menit")
                            }
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapTick()
                                    onStepMinutes(5)
                                },
                                enabled = !timer.hasActiveSession,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Text("+5")
                            }
                        }
                        timer.validationError?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                taskMenuExpanded = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("focus_link_task_button"),
                        ) {
                            val buttonIcon = linkedTask?.let {
                                TaskIconCatalog.resolveIcon(it.iconKey, it.title)
                            } ?: Icons.Filled.Link
                            Icon(buttonIcon, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = timer.linkedTaskTitle ?: "Pilih Tugas (Opsional)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        DropdownMenu(
                            expanded = taskMenuExpanded,
                            onDismissRequest = { taskMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bebas (tanpa tugas)") },
                                onClick = {
                                    haptics.tapSoft()
                                    taskMenuExpanded = false
                                    onSelectTask(null)
                                },
                            )
                            activeTasks.forEach { t ->
                                val taskIcon = TaskIconCatalog.resolveIcon(t.iconKey, t.title)
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = taskIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    text = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        haptics.tapSoft()
                                        taskMenuExpanded = false
                                        onSelectTask(t)
                                    },
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = Triple(timer.hasActiveSession, timer.isRunning, timer.validationError == null),
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.94f))
                                .togetherWith(fadeOut(tween(140)) + scaleOut(targetScale = 0.94f))
                        },
                        label = "focus_controls_morph",
                    ) { (hasActive, isRunning, isValid) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when {
                                !hasActive -> {
                                    BouncyButton(
                                        onClick = {
                                            haptics.tapConfirm()
                                            onStart()
                                        },
                                        enabled = isValid,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = atmo.accentOrange,
                                            contentColor = atmo.onAccentOrange,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("focus_start_button"),
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Mulai Fokus")
                                    }
                                }
                                isRunning -> {
                                    BouncyFilledTonalButton(
                                        onClick = {
                                            haptics.tapSoft()
                                            onPause()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("focus_pause_button"),
                                    ) {
                                        Icon(Icons.Filled.Pause, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Jeda")
                                    }
                                }
                                else -> {
                                    BouncyButton(
                                        onClick = {
                                            haptics.tapConfirm()
                                            onResume()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("focus_resume_button"),
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Lanjutkan")
                                    }
                                }
                            }

                            if (hasActive) {
                                BouncyOutlinedButton(
                                    onClick = {
                                        haptics.tapConfirm()
                                        onStopAndSave()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("focus_stop_button"),
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Selesai")
                                }
                            }

                            BouncyIconButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onReset()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_reset_button"),
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Reset Timer")
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mode Minim Gangguan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Layar tenang tanpa distraksi. Bebas keluar kapan aja.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = timer.isDistractionFreeMode,
                                onCheckedChange = {
                                    haptics.tapSoft()
                                    onToggleDistractionFree(it)
                                },
                                modifier = Modifier.testTag("distraction_free_switch"),
                            )
                        }
                    }
                }
            }
        }

        item(key = "history_title") {
            Text(
                text = "Riwayat Fokus (${state.focusSessions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (state.focusSessions.isEmpty()) {
            item(key = "empty_focus_history") {
                FrostedGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                ) {
                    Text(
                        text = "Belum ada riwayat fokus. Yuk mulai sesi pertama!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(
                items = state.focusSessions.take(15),
                key = { it.id },
            ) { session ->
                FrostedGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    cornerRadius = 18.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = session.linkedTaskTitle ?: "Sesi Fokus Bebas",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${DateTimeHelper.formatEpochTime(session.startedAtEpochMs)} • Target ${session.plannedMinutes} mnt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = DateTimeHelper.formatCountdown(session.actualSeconds),
                                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = atmo.accentOrange,
                            )
                            if (session.completedNaturally) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Tuntas penuh",
                                    tint = atmo.accentMint,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GentleFocusWorshipBanner(
    item: WorshipScheduleItem,
    onMarkDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = atmo.accentMint.copy(alpha = if (atmo.isDark) 0.16f else 0.12f),
        border = BorderStroke(1.dp, atmo.accentMint.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_gentle_worship_prompt"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Mosque,
                contentDescription = "Pengingat ibadah lembut",
                tint = atmo.accentMint,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Masuk waktu ${item.title} (${item.formattedTime})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Timer tetap jalan. Ambil jeda sebentar kapan kamu siap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BouncyTextButton(
                onClick = {
                    haptics.tapConfirm()
                    onMarkDone()
                },
                modifier = Modifier.testTag("focus_worship_mark_done"),
            ) {
                Text("Sudah", fontWeight = FontWeight.Bold)
            }
            BouncyIconButton(
                onClick = {
                    haptics.tapSoft()
                    onDismiss()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("focus_worship_dismiss"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Tutup pengingat ibadah",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DistractionFreeFocusDialog(
    state: JadwalNihUiState,
    linkedTask: ScheduleTask?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAndSave: () -> Unit,
    onExitDistractionFree: () -> Unit,
    onDismissWorshipPrompt: () -> Unit,
    onMarkWorshipDone: (WorshipScheduleItem) -> Unit,
) {
    val timer = state.focusTimer
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val pulseTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    Dialog(
        onDismissRequest = onExitDistractionFree,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onExitDistractionFree)
        AtmosphericBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Mode Minim Gangguan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            onExitDistractionFree()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("exit_distraction_free_button"),
                    ) {
                        Icon(Icons.Filled.CloseFullscreen, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Keluar")
                    }
                }

                state.focusGentleWorshipPrompt?.let { promptItem ->
                    GentleFocusWorshipBanner(
                        item = promptItem,
                        onMarkDone = { onMarkWorshipDone(promptItem) },
                        onDismiss = onDismissWorshipPrompt,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Box(
                        modifier = Modifier.size(240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = (size.minDimension / 2f) * if (timer.isRunning) pulseScale else 0.98f
                            drawCircle(
                                color = atmo.accentOrange.copy(alpha = 0.14f),
                                radius = radius,
                            )
                            drawCircle(
                                color = atmo.accentOrange,
                                radius = radius * 0.85f,
                                style = Stroke(width = 5.dp.toPx()),
                            )
                        }
                        Text(
                            text = DateTimeHelper.formatCountdown(timer.remainingSeconds),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (linkedTask != null) {
                            Icon(
                                imageVector = TaskIconCatalog.resolveIcon(linkedTask.iconKey, linkedTask.title),
                                contentDescription = null,
                                tint = atmo.accentOrange,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = timer.linkedTaskTitle ?: "Tetap hadir, selesaikan pelan-pelan.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (timer.isRunning) {
                        BouncyFilledTonalButton(
                            onClick = {
                                haptics.tapSoft()
                                onPause()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Jeda")
                        }
                    } else {
                        BouncyButton(
                            onClick = {
                                haptics.tapConfirm()
                                onResume()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (timer.hasActiveSession) "Lanjutkan" else "Mulai Fokus")
                        }
                    }

                    if (timer.hasActiveSession) {
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapConfirm()
                                onStopAndSave()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Selesai")
                        }
                    }
                }
            }
        }
    }
}
