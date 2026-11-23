package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferencesStore
import com.example.domain.NotificationCategory
import com.example.domain.NotificationLogEntry
import com.example.domain.TaskDraft
import com.example.ui.theme.LocalAtmospherePalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    logs: List<NotificationLogEntry>,
    onResumeDraft: (TaskDraft) -> Unit,
    onDeleteLog: (NotificationLogEntry) -> Unit,
    onClearAllLogs: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCategory by remember { mutableStateOf<NotificationCategory?>(null) }
    var pendingDeleteSingle by remember { mutableStateOf<NotificationLogEntry?>(null) }
    var pendingClearAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onMarkAllRead()
    }

    val filteredLogs = remember(logs, selectedCategory) {
        if (selectedCategory == null) logs else logs.filter { it.category == selectedCategory }
    }

    val dateTimeFormatter = remember {
        SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("id-ID"))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = atmo.sheetBg,
        modifier = Modifier.testTag("notification_center_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pusat Notifikasi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Riwayat 30 hari terakhir • maksimal 5 draf tersimpan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (logs.isNotEmpty()) {
                    BouncyIconButton(
                        onClick = {
                            haptics.tapSoft()
                            pendingClearAll = true
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_all_notifications_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Bersihkan semua notifikasi",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                BouncyIconButton(
                    onClick = {
                        haptics.tapSoft()
                        onDismiss()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup pusat notifikasi")
                }
            }

            // Category Filter Chips: Semua, Draf, Tugas, Ibadah, Hari Raya
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BouncyFilterChip(
                    selected = selectedCategory == null,
                    onClick = {
                        haptics.tapSoft()
                        selectedCategory = null
                    },
                    label = { Text("Semua (${logs.size})") },
                    modifier = Modifier.testTag("notif_filter_ALL"),
                )
                NotificationCategory.entries.filter { it != NotificationCategory.ALL }.forEach { cat ->
                    val count = logs.count { it.category == cat }
                    BouncyFilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            haptics.tapSoft()
                            selectedCategory = if (selectedCategory == cat) null else cat
                        },
                        label = { Text("${cat.labelId} ($count)") },
                        modifier = Modifier.testTag("notif_filter_${cat.name}"),
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                FrostedGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "Belum ada riwayat notifikasi",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Pengingat jadwal, waktu ibadah, info hari raya, dan draf yang ketutup bakal muncul di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = filteredLogs,
                        key = { it.id },
                    ) { entry ->
                        val parsedDraft = remember(entry.draftJson) {
                            entry.draftJson?.let { UserPreferencesStore.deserializeDraft(it) }
                        }
                        val (catIcon, iconBg, iconFg) = when (entry.category) {
                            NotificationCategory.DRAFT -> Triple(
                                Icons.Filled.EditNote,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            NotificationCategory.TASK -> Triple(
                                Icons.Filled.TaskAlt,
                                atmo.accentAmber.copy(alpha = 0.2f),
                                atmo.accentOrange,
                            )
                            NotificationCategory.WORSHIP -> Triple(
                                Icons.Filled.Mosque,
                                atmo.accentMint.copy(alpha = 0.22f),
                                atmo.accentMint,
                            )
                            NotificationCategory.HOLIDAY -> Triple(
                                Icons.Filled.Celebration,
                                atmo.accentCoral.copy(alpha = 0.22f),
                                atmo.accentCoral,
                            )
                            NotificationCategory.ALL -> Triple(
                                Icons.Filled.Notifications,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (parsedDraft != null) {
                                        Modifier.clickable {
                                            haptics.tapSoft()
                                            onResumeDraft(parsedDraft)
                                            onDismiss()
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .testTag("notification_item_${entry.id}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    color = iconBg,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = entry.category.labelId,
                                            tint = iconFg,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = entry.category.labelId,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = dateTimeFormatter.format(Date(entry.createdAtEpochMs)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = entry.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (parsedDraft != null) {
                                        BouncyOutlinedButton(
                                            onClick = {
                                                haptics.tapConfirm()
                                                onResumeDraft(parsedDraft)
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .minimumInteractiveComponentSize()
                                                .testTag("notification_resume_draft_${entry.id}"),
                                        ) {
                                            Text(
                                                text = "Lanjutkan Draf",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            )
                                        }
                                    }
                                }

                                BouncyIconButton(
                                    onClick = {
                                        haptics.tapSoft()
                                        pendingDeleteSingle = entry
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_notification_${entry.id}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Hapus notifikasi",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val targetSingle = pendingDeleteSingle
    if (targetSingle != null) {
        ConfirmDeleteDialog(
            title = "Hapus notifikasi ini?",
            message = "Riwayat \"${targetSingle.title}\" bakal dihapus dari Pusat Notifikasi.",
            onConfirm = {
                onDeleteLog(targetSingle)
                pendingDeleteSingle = null
            },
            onDismiss = { pendingDeleteSingle = null },
        )
    }

    if (pendingClearAll) {
        ConfirmDeleteDialog(
            title = "Bersihkan semua notifikasi?",
            message = "Semua riwayat notifikasi dan daftar draf di Pusat Notifikasi bakal dihapus.",
            confirmLabel = "Ya, Bersihkan",
            onConfirm = {
                onClearAllLogs()
                pendingClearAll = false
            },
            onDismiss = { pendingClearAll = false },
        )
    }
}
