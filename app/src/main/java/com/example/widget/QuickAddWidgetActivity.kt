package com.example.widget

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.ImageStorageHelper
import com.example.data.JadwalNihRepository
import com.example.data.JsonBackupManager
import com.example.data.UserPreferencesStore
import com.example.domain.DateTimeHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.ScheduleTask
import com.example.domain.TaskIconCatalog
import com.example.domain.ThemeMode
import com.example.domain.UserPreferences
import com.example.notifications.ReminderScheduler
import com.example.ui.components.BouncyButton
import com.example.ui.components.BouncyFilterChip
import com.example.ui.components.BouncyIconButton
import com.example.ui.components.BouncyOutlinedButton
import com.example.ui.components.bouncyPress
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.tapConfirm
import com.example.ui.components.tapSoft
import com.example.ui.components.tapTick
import com.example.ui.components.tapWarning
import com.example.ui.theme.JadwalNihTheme
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle
import kotlinx.coroutines.launch

class QuickAddWidgetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val defaultQuadrantName = intent.getStringExtra(EXTRA_DEFAULT_QUADRANT)
        val initialQuadrant = PriorityQuadrant.entries.firstOrNull { it.name == defaultQuadrantName }
            ?: PriorityQuadrant.Q1_IMPORTANT_URGENT
        val prefsStore = UserPreferencesStore(applicationContext)

        setContent {
            val prefs by prefsStore.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val scope = rememberCoroutineScope()

            JadwalNihTheme(darkTheme = darkTheme) {
                QuickAddWidgetPopup(
                    initialQuadrant = initialQuadrant,
                    onDismiss = { finish() },
                    onSaveTask = { task ->
                        scope.launch {
                            val db = AppDatabase.getInstance(applicationContext)
                            val imageHelper = ImageStorageHelper(applicationContext)
                            val backupManager = JsonBackupManager(
                                context = applicationContext,
                                taskDao = db.scheduleTaskDao(),
                                focusDao = db.focusSessionDao(),
                                recapDao = db.photoRecapDao(),
                                imageHelper = imageHelper,
                            )
                            val repo = JadwalNihRepository(
                                taskDao = db.scheduleTaskDao(),
                                focusDao = db.focusSessionDao(),
                                recapDao = db.photoRecapDao(),
                                imageHelper = imageHelper,
                                backupManager = backupManager,
                                notificationLogDao = db.notificationLogDao(),
                                worshipCheckinDao = db.worshipCheckinDao(),
                            )
                            val enrichedTask = task.copy(
                                iconKey = task.iconKey.ifBlank { TaskIconCatalog.autoDetectIconKey(task.title) },
                                locationName = task.locationName.ifBlank { prefs.lastLocationName },
                                locationLat = task.locationLat ?: prefs.lastLatitude,
                                locationLng = task.locationLng ?: prefs.lastLongitude,
                            )
                            val savedId = repo.upsertTask(enrichedTask)
                            val persisted = enrichedTask.copy(id = savedId)
                            if (prefs.checkInNotificationsEnabled || persisted.reminderEnabled) {
                                ReminderScheduler(applicationContext).scheduleForTask(
                                    persisted,
                                    checkInNotificationsEnabled = prefs.checkInNotificationsEnabled,
                                )
                            }
                            WidgetSyncHelper.refreshAll(applicationContext)
                            finish()
                        }
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_DEFAULT_QUADRANT = "extra_default_quadrant"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddWidgetPopup(
    initialQuadrant: PriorityQuadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
    onDismiss: () -> Unit,
    onSaveTask: (ScheduleTask) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val atmo = LocalAtmospherePalette.current
    val statusPalette = LocalStatusPalette.current
    val todayIso = remember { DateTimeHelper.todayIso() }

    val defaultStart = remember {
        ((DateTimeHelper.nowMinuteOfDay() / 15 + 1) * 15).coerceIn(0, 22 * 60 + 45)
    }
    val defaultEnd = remember(defaultStart) {
        (defaultStart + 60).coerceAtMost(23 * 60 + 45)
    }

    var title by remember { mutableStateOf("") }
    var quadrant by remember(initialQuadrant) { mutableStateOf(initialQuadrant) }
    var startMin by remember { mutableIntStateOf(defaultStart) }
    var endMin by remember { mutableIntStateOf(defaultEnd) }
    var formError by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Surface(
            color = atmo.sheetBg,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, atmo.cardBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .testTag("widget_quick_add_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AddTask,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Quick-Add Prioritas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Hari Ini • ${DateTimeHelper.formatShortDate(todayIso)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    BouncyIconButton(
                        onClick = {
                            haptics.tapSoft()
                            onDismiss()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("widget_quick_add_close"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup Quick-Add",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        formError = null
                    },
                    label = { Text("Judul Kegiatan") },
                    placeholder = { Text("Misal: Selesaikan laporan utama") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shakeOnTrigger(shakeTrigger)
                        .testTag("widget_quick_add_title_input"),
                )

                // Kuadran K1-K4 Selector (2x2 compact grid)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kuadran Prioritas",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    PriorityQuadrant.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { q ->
                                val selected = quadrant == q
                                val (solidBg, solidFg, softBg, softFg) = when (q) {
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
                                val bg by animateColorAsState(
                                    targetValue = if (selected) solidBg else softBg,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "qa_q_bg_${q.shortCode}",
                                )
                                val fg by animateColorAsState(
                                    targetValue = if (selected) solidFg else softFg,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "qa_q_fg_${q.shortCode}",
                                )
                                val interaction = remember(q) { MutableInteractionSource() }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .bouncyPress(interaction, pressedScale = 0.94f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(
                                            interactionSource = interaction,
                                            indication = null,
                                        ) {
                                            haptics.tapTick()
                                            quadrant = q
                                        }
                                        .minimumInteractiveComponentSize()
                                        .testTag("widget_quick_add_quadrant_${q.shortCode}"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = bg,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, solidBg),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "${q.shortCode} • ${q.chipLabelId}",
                                                style = MonoSmallStyle.copy(fontWeight = FontWeight.ExtraBold),
                                                color = fg,
                                            )
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Terpilih",
                                                    tint = fg,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                        Text(
                                            text = q.subtitleId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = fg.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Jam Mulai & Selesai + Preset Durasi
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Waktu Hari Ini",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val durMins = (endMin - startMin)
                        if (durMins > 0) {
                            Text(
                                text = DateTimeHelper.formatTimelineDuration(durMins),
                                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = atmo.accentCobalt,
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
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        haptics.tapTick()
                                        val nextStart = hour * 60 + minute
                                        startMin = nextStart
                                        if (endMin <= nextStart) {
                                            endMin = (nextStart + 30).coerceAtMost(23 * 60 + 59)
                                        }
                                        formError = null
                                    },
                                    startMin / 60,
                                    startMin % 60,
                                    true,
                                ).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("widget_quick_add_start_time"),
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mulai ${DateTimeHelper.formatMinuteOfDay(startMin)}", style = MonoSmallStyle)
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("widget_quick_add_end_time"),
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Selesai ${DateTimeHelper.formatMinuteOfDay(endMin)}", style = MonoSmallStyle)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(30 to "30 mnt", 60 to "1 jam", 120 to "2 jam").forEach { (mins, label) ->
                            val selected = (endMin - startMin) == mins
                            BouncyFilterChip(
                                selected = selected,
                                onClick = {
                                    haptics.tapTick()
                                    endMin = (startMin + mins).coerceAtMost(23 * 60 + 59)
                                    formError = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                label = { Text(label) },
                                modifier = Modifier.testTag("widget_quick_add_dur_$mins"),
                            )
                        }
                    }
                }

                formError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("widget_quick_add_error"),
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(0.4f)
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("widget_quick_add_cancel"),
                    ) {
                        Text("Batal")
                    }
                    BouncyButton(
                        onClick = {
                            val trimmed = title.trim()
                            val dur = endMin - startMin
                            when {
                                trimmed.isEmpty() -> {
                                    haptics.tapWarning()
                                    shakeTrigger += 1
                                    formError = "Isi judul kegiatan dulu ya."
                                }
                                dur <= 0 -> {
                                    haptics.tapWarning()
                                    shakeTrigger += 1
                                    formError = "Jam selesai harus setelah jam mulai."
                                }
                                else -> {
                                    haptics.tapConfirm()
                                    onSaveTask(
                                        ScheduleTask(
                                            title = trimmed,
                                            dateIso = todayIso,
                                            startMinuteOfDay = startMin,
                                            endMinuteOfDay = endMin,
                                            estimatedMinutes = dur,
                                            quadrant = quadrant,
                                            reminderEnabled = true,
                                            reminderLeadMinutes = 10,
                                        ),
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = atmo.accentCobalt,
                            contentColor = atmo.onAccentCobalt,
                        ),
                        modifier = Modifier
                            .weight(0.6f)
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("widget_quick_add_save"),
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
