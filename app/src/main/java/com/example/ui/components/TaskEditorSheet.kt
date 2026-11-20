package com.example.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.DateTimeHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleTask
import com.example.ui.theme.MonoSmallStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    initialTask: ScheduleTask?,
    defaultDateIso: String,
    onDismiss: () -> Unit,
    onSave: (ScheduleTask) -> Unit,
    onNotifyPermissionDenied: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var dateIso by remember(initialTask, defaultDateIso) {
        mutableStateOf(initialTask?.dateIso ?: defaultDateIso)
    }
    var startMin by remember(initialTask) {
        mutableIntStateOf(initialTask?.startMinuteOfDay ?: ((DateTimeHelper.nowMinuteOfDay() / 30 + 1) * 30).coerceAtMost(21 * 60))
    }
    var endMin by remember(initialTask) {
        mutableIntStateOf(initialTask?.endMinuteOfDay ?: (startMin + 60).coerceAtMost(23 * 60 + 45))
    }
    var estimatedInput by remember(initialTask) {
        mutableStateOf((initialTask?.estimatedMinutes ?: (endMin - startMin).coerceAtLeast(15)).toString())
    }
    var quadrant by remember(initialTask) {
        mutableStateOf(initialTask?.quadrant ?: PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT)
    }
    var isFlexible by remember(initialTask) {
        mutableStateOf(initialTask?.isFlexible ?: false)
    }
    var recurrence by remember(initialTask) {
        mutableStateOf(initialTask?.recurrence ?: RecurrenceRule.NONE)
    }
    var reminderEnabled by remember(initialTask) {
        mutableStateOf(initialTask?.reminderEnabled ?: false)
    }
    var reminderLeadMinutes by remember(initialTask) {
        mutableIntStateOf(initialTask?.reminderLeadMinutes ?: 10)
    }
    var formError by remember { mutableStateOf<String?>(null) }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (initialTask == null) "Tambah Jadwal" else "Edit Jadwal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

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
                    .testTag("task_title_input"),
            )

            // Date selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tanggal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val today = DateTimeHelper.todayIso()
                val tomorrow = DateTimeHelper.addDays(today, 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = dateIso == today,
                        onClick = { dateIso = today },
                        label = { Text("Hari Ini") },
                    )
                    FilterChip(
                        selected = dateIso == tomorrow,
                        onClick = { dateIso = tomorrow },
                        label = { Text("Besok") },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { dateIso = DateTimeHelper.addDays(dateIso, -1) }) {
                        Text("-1 Hari")
                    }
                    TextButton(onClick = { dateIso = DateTimeHelper.addDays(dateIso, 1) }) {
                        Text("+1 Hari")
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        )
                    }
                }
            }

            // Start & End Time Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val newStart = hour * 60 + minute
                                startMin = newStart
                                if (endMin <= newStart) {
                                    endMin = (newStart + 30).coerceAtMost(23 * 60 + 59)
                                }
                                estimatedInput = (endMin - startMin).coerceAtLeast(15).toString()
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

                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                endMin = hour * 60 + minute
                                if (endMin > startMin) {
                                    estimatedInput = (endMin - startMin).toString()
                                }
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

            OutlinedTextField(
                value = estimatedInput,
                onValueChange = { raw ->
                    estimatedInput = raw.filter { it.isDigit() }.take(3)
                    formError = null
                },
                label = { Text("Durasi (menit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 4-Quadrant Matrix selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Prioritas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PriorityQuadrant.entries.forEach { q ->
                    val selected = quadrant == q
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { quadrant = q },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${q.shortCode} — ${q.titleId}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = q.subtitleId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceRule.entries.forEach { rule ->
                        FilterChip(
                            selected = recurrence == rule,
                            onClick = { recurrence = rule },
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
                    Text("Waktu Fleksibel", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Tandai biru kalau jamnya longgar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isFlexible, onCheckedChange = { isFlexible = it })
            }

            // Reminder Notification Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pengingat", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Kirim notif sebelum mulai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { checked ->
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

            if (reminderEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = reminderLeadMinutes == mins,
                            onClick = { reminderLeadMinutes = mins },
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
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        val trimmed = title.trim()
                        val est = estimatedInput.toIntOrNull()
                        when {
                            trimmed.isEmpty() -> formError = "Isi judul dulu ya."
                            endMin <= startMin -> formError = "Jam selesai harus lebih akhir."
                            est == null || est <= 0 -> formError = "Durasi minimal 1 menit."
                            else -> {
                                onSave(
                                    ScheduleTask(
                                        id = initialTask?.id ?: 0L,
                                        title = trimmed,
                                        dateIso = dateIso,
                                        startMinuteOfDay = startMin,
                                        endMinuteOfDay = endMin,
                                        estimatedMinutes = est,
                                        quadrant = quadrant,
                                        isCompleted = initialTask?.isCompleted ?: false,
                                        isFlexible = isFlexible,
                                        recurrence = recurrence,
                                        reminderEnabled = reminderEnabled,
                                        reminderLeadMinutes = reminderLeadMinutes,
                                        completedAtEpochMs = initialTask?.completedAtEpochMs,
                                        createdAtEpochMs = initialTask?.createdAtEpochMs ?: System.currentTimeMillis(),
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
