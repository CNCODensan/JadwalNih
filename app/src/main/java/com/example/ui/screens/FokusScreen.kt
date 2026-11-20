package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.JadwalNihUiState
import com.example.ui.components.AtmosphericBackground
import com.example.ui.components.FrostedGlassCard
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
) {
    val timer = state.focusTimer
    val atmo = LocalAtmospherePalette.current
    var taskMenuExpanded by remember { mutableStateOf(false) }
    val activeTasks = remember(state.allTasksWithStatus) {
        state.allTasksWithStatus.filterNot { it.task.isCompleted }.map { it.task }
    }

    if (timer.isDistractionFreeMode) {
        DistractionFreeFocusDialog(
            state = state,
            onPause = onPause,
            onResume = onResume,
            onStopAndSave = onStopAndSave,
            onExitDistractionFree = { onToggleDistractionFree(false) },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "focus_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Timer Fokus",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Atur durasi bebas 1–240 menit, tanpa aturan kaku.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "timer_card") {
            FrostedGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val totalSecs = (timer.plannedMinutes * 60).coerceAtLeast(1)
                    val progress = (timer.remainingSeconds.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)

                    Box(
                        modifier = Modifier.size(216.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            drawCircle(
                                color = atmo.accentPurple.copy(alpha = 0.08f),
                                radius = size.minDimension / 2f,
                            )
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth),
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(atmo.accentBlue, atmo.accentPurple, atmo.accentBlue),
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
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
                            Text(
                                text = when {
                                    timer.isRunning -> "Sedang fokus..."
                                    timer.isPaused -> "Dijeda"
                                    else -> "Target ${timer.plannedMinutes} menit"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Durasi (Menit)",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onStepMinutes(-5) },
                                enabled = !timer.hasActiveSession,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Text("-5")
                            }
                            IconButton(
                                onClick = { onStepMinutes(-1) },
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
                            IconButton(
                                onClick = { onStepMinutes(1) },
                                enabled = !timer.hasActiveSession,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Tambah 1 menit")
                            }
                            OutlinedButton(
                                onClick = { onStepMinutes(5) },
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
                        OutlinedButton(
                            onClick = { taskMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("focus_link_task_button"),
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null)
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
                                    taskMenuExpanded = false
                                    onSelectTask(null)
                                },
                            )
                            activeTasks.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        taskMenuExpanded = false
                                        onSelectTask(t)
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when {
                            !timer.hasActiveSession -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = if (timer.validationError == null) {
                                                    listOf(atmo.heroGradStart, atmo.heroGradEnd)
                                                } else {
                                                    listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f))
                                                },
                                            ),
                                        ),
                                ) {
                                    Button(
                                        onClick = onStart,
                                        enabled = timer.validationError == null,
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = Color.White,
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("focus_start_button"),
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Mulai Fokus")
                                    }
                                }
                            }
                            timer.isRunning -> {
                                FilledTonalButton(
                                    onClick = onPause,
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
                                Button(
                                    onClick = onResume,
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

                        if (timer.hasActiveSession) {
                            OutlinedButton(
                                onClick = onStopAndSave,
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

                        IconButton(
                            onClick = onReset,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("focus_reset_button"),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset Timer")
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
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
                                )
                                Text(
                                    text = "Layar tenang tanpa distraksi. Bebas keluar kapan aja.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = timer.isDistractionFreeMode,
                                onCheckedChange = onToggleDistractionFree,
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
            )
        }

        if (state.focusSessions.isEmpty()) {
            item(key = "empty_focus_history") {
                FrostedGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
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
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
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
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (session.completedNaturally) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Tuntas penuh",
                                    tint = MaterialTheme.colorScheme.secondary,
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
private fun DistractionFreeFocusDialog(
    state: JadwalNihUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAndSave: () -> Unit,
    onExitDistractionFree: () -> Unit,
) {
    val timer = state.focusTimer
    val atmo = LocalAtmospherePalette.current
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
                    OutlinedButton(
                        onClick = onExitDistractionFree,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("exit_distraction_free_button"),
                    ) {
                        Icon(Icons.Filled.CloseFullscreen, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Keluar")
                    }
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
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        atmo.accentPurple.copy(alpha = 0.24f),
                                        atmo.accentBlue.copy(alpha = 0.12f),
                                        Color.Transparent,
                                    ),
                                ),
                                radius = radius,
                            )
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(atmo.accentBlue, atmo.accentPurple, atmo.accentBlue),
                                ),
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

                    Text(
                        text = timer.linkedTaskTitle ?: "Tetap hadir, selesaikan pelan-pelan.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (timer.isRunning) {
                        FilledTonalButton(
                            onClick = onPause,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Jeda")
                        }
                    } else {
                        Button(
                            onClick = onResume,
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
                        OutlinedButton(
                            onClick = onStopAndSave,
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
