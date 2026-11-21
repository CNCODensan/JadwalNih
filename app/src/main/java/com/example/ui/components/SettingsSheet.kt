package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.QuickGlanceTarget
import com.example.ui.theme.LocalAtmospherePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    userName: String,
    isDarkMode: Boolean,
    quickGlanceTarget: QuickGlanceTarget,
    checkInNotificationsEnabled: Boolean,
    isBusyBackup: Boolean,
    onUserNameChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onQuickGlanceTargetChange: (QuickGlanceTarget) -> Unit,
    onCheckInNotificationsChange: (Boolean) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onResetDataConfirmed: () -> Unit,
    onNotifyPermissionDenied: () -> Unit,
    onDismiss: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfirmReset by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onCheckInNotificationsChange(true)
        } else {
            onCheckInNotificationsChange(false)
            onNotifyPermissionDenied()
        }
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            containerColor = atmo.sheetBg,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Reset Semua Data?") },
            text = {
                Text(
                    "Semua jadwal, sesi fokus, rekap foto, dan preferensi akan dihapus permanen. Ekspor JSON dulu kalau masih butuh.",
                )
            },
            confirmButton = {
                BouncyButton(
                    onClick = {
                        haptics.tapConfirm()
                        showConfirmReset = false
                        onResetDataConfirmed()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.testTag("confirm_reset_data_button"),
                ) {
                    Text("Ya, Reset")
                }
            },
            dismissButton = {
                BouncyTextButton(
                    onClick = {
                        haptics.tapSoft()
                        showConfirmReset = false
                    },
                ) {
                    Text("Batal")
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Nama Pengguna untuk sapaan Quick Glance
            Surface(
                color = atmo.sheetCardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = atmo.accentCobalt,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Nama Panggilan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    OutlinedTextField(
                        value = userName,
                        onValueChange = onUserNameChange,
                        placeholder = { Text("Misal: Devan") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input"),
                    )
                    Text(
                        text = "Ditampilkan di sapaan Ringkasan Cepat tab Jadwal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Tema & Tampilan
            Surface(
                color = atmo.sheetCardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = if (isDarkMode) atmo.accentAmber else atmo.accentCobalt,
                            )
                            Column {
                                Text(
                                    text = "Mode Gelap",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (isDarkMode) "Aktif (Midnight Slate)" else "Nonaktif (Terang Bersih)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = {
                                haptics.tapSoft()
                                onDarkModeChange(it)
                            },
                            modifier = Modifier.testTag("settings_theme_switch"),
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Arah ketuk Quick Glance
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Arah Ketuk Quick Glance",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Pilih tab tujuan saat kamu mengetuk kartu Ringkasan Cepat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            QuickGlanceTarget.entries.forEach { target ->
                                val selected = quickGlanceTarget == target
                                val icon = if (target == QuickGlanceTarget.ANALITIK) {
                                    Icons.Filled.BarChart
                                } else {
                                    Icons.Filled.GridView
                                }
                                BouncyFilterChip(
                                    selected = selected,
                                    onClick = {
                                        haptics.tapSoft()
                                        onQuickGlanceTargetChange(target)
                                    },
                                    leadingIcon = {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text(target.labelId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_glance_target_${target.name.lowercase()}"),
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Notifikasi Konfirmasi Kehadiran
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
                                tint = atmo.accentMint,
                            )
                            Column {
                                Text(
                                    text = "Notif Konfirmasi Acara",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Kirim notifikasi saat jadwal mulai agar bisa langsung konfirmasi hadir.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = checkInNotificationsEnabled,
                            onCheckedChange = { checked ->
                                haptics.tapSoft()
                                if (!checked) {
                                    onCheckInNotificationsChange(false)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        onCheckInNotificationsChange(true)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    onCheckInNotificationsChange(true)
                                }
                            },
                            modifier = Modifier.testTag("settings_checkin_notif_switch"),
                        )
                    }
                }
            }

            // Cadangan Data & Reset
            Surface(
                color = atmo.sheetCardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Cadangan Data (JSON)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Semua jadwal, sesi fokus, dan foto rekap tersimpan di perangkat. Ekspor kapan saja.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (isBusyBackup) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Memproses JSON...", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BouncyButton(
                            onClick = {
                                haptics.tapSoft()
                                onExportJson()
                            },
                            enabled = !isBusyBackup,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("export_json_button"),
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ekspor")
                        }
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                onImportJson()
                            },
                            enabled = !isBusyBackup,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("import_json_button"),
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Impor")
                        }
                    }

                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapWarning()
                            showConfirmReset = true
                        },
                        enabled = !isBusyBackup,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("reset_data_button"),
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset Semua Data")
                    }
                }
            }

            BouncyTextButton(
                onClick = {
                    haptics.tapSoft()
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Tutup")
            }
        }
    }
}
