package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.DateTimeHelper
import com.example.domain.HolidayThemeType
import com.example.domain.IndonesiaRegency
import com.example.domain.LocationHelper
import com.example.domain.PersonalBirthdayStats
import com.example.domain.PrayerCardVisibilityMode
import com.example.domain.QuickGlanceTarget
import com.example.domain.ReligionOption
import com.example.domain.ThemeMode
import com.example.ui.theme.LocalAtmospherePalette

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    userName: String,
    themeMode: ThemeMode,
    systemInDarkTheme: Boolean = false,
    isSimpleMode: Boolean = false,
    quickGlanceTarget: QuickGlanceTarget,
    checkInNotificationsEnabled: Boolean,
    isBusyBackup: Boolean,
    onUserNameChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSimpleModeChange: (Boolean) -> Unit = {},
    onQuickGlanceTargetChange: (QuickGlanceTarget) -> Unit,
    onCheckInNotificationsChange: (Boolean) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onResetDataConfirmed: () -> Unit,
    onNotifyPermissionDenied: () -> Unit,
    onDismiss: () -> Unit,
    religion: ReligionOption = ReligionOption.ISLAM,
    prayerReminderEnabled: Boolean = true,
    worshipNotificationSoundEnabled: Boolean = true,
    holidayThemeEnabled: Boolean = true,
    holidayThemePreview: HolidayThemeType = HolidayThemeType.NONE,
    currentLocationName: String = "",
    onReligionChange: (ReligionOption) -> Unit = {},
    onPrayerReminderEnabledChange: (Boolean) -> Unit = {},
    onWorshipSoundChange: (Boolean) -> Unit = {},
    onHolidayThemeEnabledChange: (Boolean) -> Unit = {},
    onHolidayThemePreviewChange: (HolidayThemeType) -> Unit = {},
    onDetectLocation: () -> Unit = {},
    onManualLocationSave: (String) -> Unit = {},
    onRegionSelected: (IndonesiaRegency) -> Unit = {},
    birthDateIso: String = "",
    personalBirthdayStats: PersonalBirthdayStats = PersonalBirthdayStats(),
    prayerCardVisibilityMode: PrayerCardVisibilityMode = PrayerCardVisibilityMode.ONLY_ACTIVE_WINDOW,
    allowMissedPrayerManualEdit: Boolean = true,
    currentLatitude: Double? = null,
    currentLongitude: Double? = null,
    onBirthDateSave: (String) -> Unit = {},
    onTriggerBirthdayPreview: () -> Unit = {},
    onPrayerCardVisibilityChange: (PrayerCardVisibilityMode) -> Unit = {},
    onAllowMissedPrayerEditChange: (Boolean) -> Unit = {},
    onOpenWorshipHistory: () -> Unit = {},
    onConfirmMapLocation: (String, Double, Double) -> Unit = { _, _, _ -> },
    onReplayHolidayMotion: () -> Unit = {},
) {
    val atmo = LocalAtmospherePalette.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfirmReset by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var manualLocationDraft by remember(currentLocationName) { mutableStateOf(currentLocationName) }

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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onDetectLocation()
        }
    }

    if (showRegionPicker) {
        IndonesiaRegionPickerSheet(
            onSelectRegency = { regency ->
                manualLocationDraft = regency.displayFullName
                onRegionSelected(regency)
            },
            onDismiss = { showRegionPicker = false },
        )
    }

    if (showMapPicker) {
        MapLocationPickerSheet(
            initialLocationName = currentLocationName,
            initialLatitude = currentLatitude,
            initialLongitude = currentLongitude,
            onConfirmLocation = { name, lat, lng ->
                manualLocationDraft = name
                onConfirmMapLocation(name, lat, lng)
            },
            onDismiss = { showMapPicker = false },
        )
    }

    if (showBirthDatePicker) {
        val initialBirthIso = birthDateIso.takeIf { it.isNotBlank() } ?: "2000-01-01"
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeHelper.isoToUtcDatePickerMillis(initialBirthIso),
        )
        DatePickerDialog(
            onDismissRequest = { showBirthDatePicker = false },
            confirmButton = {
                BouncyTextButton(
                    onClick = {
                        haptics.tapConfirm()
                        datePickerState.selectedDateMillis?.let { millis ->
                            onBirthDateSave(DateTimeHelper.utcDatePickerMillisToIso(millis))
                        }
                        showBirthDatePicker = false
                    },
                    modifier = Modifier.testTag("settings_confirm_birthdate_button"),
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                BouncyTextButton(onClick = { showBirthDatePicker = false }) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            shape = MaterialTheme.shapes.large,
            containerColor = atmo.sheetBg,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = {
                Icon(
                    imageVector = Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = atmo.accentCobalt,
                )
            },
            title = { Text("Info Privasi & Izin JadwalNih") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Kamera & Galeri: Dipakai hanya saat kamu mengambil atau memilih foto untuk Rekap Foto. Semua foto tersimpan lokal di perangkatmu.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Lokasi Perkiraan (GPS): Dipakai untuk menamai lokasi jadwal, catatan lokasi Rekap Foto, dan menentukan jam sholat akurat. Kamu tetap bisa ketik kota manual.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Notifikasi: Dipakai buat pengingat jadwal, konfirmasi kehadiran, dan pengingat waktu ibadah sesuai pilihanmu.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Data Hari Raya & Waktu Sholat: Mengambil jadwal umum & waktu sholat publik, dengan cadangan offline bila tanpa internet.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                BouncyButton(
                    onClick = { showPrivacyDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("close_privacy_dialog_button"),
                ) {
                    Text("Mengerti")
                }
            },
        )
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            shape = MaterialTheme.shapes.large,
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
                    shape = RoundedCornerShape(14.dp),
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
                shape = RoundedCornerShape(20.dp),
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
                        placeholder = { Text("Tulis nama panggilanmu (opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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

            // Tanggal Lahir & Perayaan Ulang Tahun
            Surface(
                color = atmo.sheetCardBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.testTag("settings_birthday_card"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cake,
                            contentDescription = null,
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ulang Tahun & Perayaan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Otomatis jadwalkan ulang tahun tahunan dan aktifkan tema kue, lilin & statistik.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                showBirthDatePicker = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_pick_birthdate_button"),
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (birthDateIso.isNotBlank()) {
                                    DateTimeHelper.formatIndonesianFullDate(birthDateIso)
                                } else {
                                    "Atur Tanggal Lahir"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (birthDateIso.isNotBlank()) {
                            BouncyTextButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onBirthDateSave("")
                                },
                                modifier = Modifier.testTag("settings_clear_birthdate_button"),
                            ) {
                                Text("Hapus", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Text(
                        text = "Memakai JadwalNih: ${personalBirthdayStats.usageDurationLabel} • ${personalBirthdayStats.completedTasksCount} tugas tuntas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapConfirm()
                            onDismiss()
                            onTriggerBirthdayPreview()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_preview_birthday_button"),
                    ) {
                        Icon(Icons.Filled.Cake, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pratinjau Kartu & Tema Ulang Tahun")
                    }
                }
            }

            // Tema & Tampilan
            Surface(
                color = atmo.sheetCardBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val activeSystemLabel = if (systemInDarkTheme) "Gelap" else "Terang"
                        val themeStatusDesc = when (themeMode) {
                            ThemeMode.SYSTEM -> "Mengikuti sistem ($activeSystemLabel) • Rekap Foto tetap gelap"
                            ThemeMode.LIGHT -> "Mode Terang aktif • Tab Rekap Foto tetap gelap"
                            ThemeMode.DARK -> "Mode Gelap Midnight Slate aktif"
                        }
                        val headerIcon = when (themeMode) {
                            ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Filled.LightMode
                            ThemeMode.DARK -> Icons.Filled.DarkMode
                        }
                        val headerTint = when (themeMode) {
                            ThemeMode.SYSTEM -> atmo.accentCobalt
                            ThemeMode.LIGHT -> atmo.accentOrange
                            ThemeMode.DARK -> atmo.accentAmber
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = headerTint,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tema Tampilan",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = themeStatusDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemeOptionPill(
                                label = "Sistem",
                                icon = Icons.Filled.BrightnessAuto,
                                accentColor = atmo.accentCobalt,
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = {
                                    haptics.tapSoft()
                                    onThemeModeChange(ThemeMode.SYSTEM)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_theme_system"),
                            )
                            ThemeOptionPill(
                                label = "Terang",
                                icon = Icons.Filled.LightMode,
                                accentColor = atmo.accentOrange,
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = {
                                    haptics.tapSoft()
                                    onThemeModeChange(ThemeMode.LIGHT)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_theme_light"),
                            )
                            ThemeOptionPill(
                                label = "Gelap",
                                icon = Icons.Filled.DarkMode,
                                accentColor = atmo.accentAmber,
                                selected = themeMode == ThemeMode.DARK,
                                onClick = {
                                    haptics.tapSoft()
                                    onThemeModeChange(ThemeMode.DARK)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_theme_dark"),
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Mode Tampilan: Mode Normal vs Mode Simpel
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
                                imageVector = Icons.Filled.Spa,
                                contentDescription = null,
                                tint = atmo.accentMint,
                            )
                            Column {
                                Text(
                                    text = "Mode Simpel",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (isSimpleMode) {
                                        "Aktif (Hanya Jadwal, Fokus & Rekap)"
                                    } else {
                                        "Nonaktif (Mode Normal lengkap)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = isSimpleMode,
                            onCheckedChange = {
                                haptics.tapSoft()
                                onSimpleModeChange(it)
                            },
                            modifier = Modifier.testTag("settings_simple_mode_switch"),
                        )
                    }

                    if (!isSimpleMode) {
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
                                        shape = RoundedCornerShape(14.dp),
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

            // Ibadah & Pengaturan Agama
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mosque,
                                contentDescription = null,
                                tint = atmo.accentMint,
                            )
                            Column {
                                Text(
                                    text = "Pengingat Ibadah / Sholat",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Tampilkan kartu & pengingat ibadah sesuai agama pilihanmu.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = prayerReminderEnabled,
                            onCheckedChange = {
                                haptics.tapSoft()
                                onPrayerReminderEnabledChange(it)
                            },
                            modifier = Modifier.testTag("settings_worship_switch"),
                        )
                    }

                    Text(
                        text = "Pilihan Agama",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ReligionOption.entries.forEach { option ->
                            BouncyFilterChip(
                                selected = religion == option,
                                onClick = {
                                    haptics.tapSoft()
                                    onReligionChange(option)
                                },
                                label = { Text(option.labelId) },
                                modifier = Modifier.testTag("settings_religion_${option.name}"),
                            )
                        }
                    }

                    if (prayerReminderEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = atmo.accentCobalt,
                                )
                                Column {
                                    Text(
                                        text = "Suara Notif Ibadah",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (worshipNotificationSoundEnabled) {
                                            "Pakai suara pengingat"
                                        } else {
                                            "Mode senyap (tampil notifikasi tanpa suara)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = worshipNotificationSoundEnabled,
                                onCheckedChange = {
                                    haptics.tapSoft()
                                    onWorshipSoundChange(it)
                                },
                                modifier = Modifier.testTag("settings_worship_sound_switch"),
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "Tampilan Kartu Sholat/Ibadah",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PrayerCardVisibilityMode.entries.forEach { mode ->
                                BouncyFilterChip(
                                    selected = prayerCardVisibilityMode == mode,
                                    onClick = {
                                        haptics.tapSoft()
                                        onPrayerCardVisibilityChange(mode)
                                    },
                                    label = { Text(mode.labelId) },
                                    modifier = Modifier.testTag("settings_prayer_visibility_${mode.name}"),
                                )
                            }
                        }
                        Text(
                            text = when (prayerCardVisibilityMode) {
                                PrayerCardVisibilityMode.ONLY_ACTIVE_WINDOW ->
                                    "Kartu disembunyikan penuh saat di luar rentang waktu sholat alami."
                                PrayerCardVisibilityMode.COMPACT_COUNTDOWN ->
                                    "Menampilkan hitung mundur menuju waktu ibadah berikutnya."
                                PrayerCardVisibilityMode.ALWAYS_SHOW ->
                                    "Menampilkan daftar lengkap seluruh waktu ibadah."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Edit Manual Sholat Lewat (7 Hari)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (allowMissedPrayerManualEdit) {
                                        "Bisa centang manual ibadah terlewat hingga 7 hari lalu."
                                    } else {
                                        "Terkunci — hari lalu hanya bisa dibaca."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = allowMissedPrayerManualEdit,
                                onCheckedChange = {
                                    haptics.tapSoft()
                                    onAllowMissedPrayerEditChange(it)
                                },
                                modifier = Modifier.testTag("settings_missed_prayer_lock_switch"),
                            )
                        }

                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                onDismiss()
                                onOpenWorshipHistory()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("settings_open_worship_history"),
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buka Riwayat Ibadah (7 Hari)")
                        }
                    }
                }
            }

            // Lokasi GPS & Kota Manual
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = atmo.accentCobalt,
                            modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(
                                text = "Lokasi GPS & Kota",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Dipakai untuk jadwal, foto Rekap, dan waktu sholat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = manualLocationDraft,
                            onValueChange = { manualLocationDraft = it },
                            placeholder = { Text("Isi kota manual (mis. Jakarta)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_manual_location_input"),
                        )
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapConfirm()
                                onManualLocationSave(manualLocationDraft)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_save_location_button"),
                        ) {
                            Text("Simpan")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                showMapPicker = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_open_map_picker_button"),
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pilih di Peta", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                showRegionPicker = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_pick_region_button"),
                        ) {
                            Icon(Icons.Filled.LocationCity, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kota Indonesia", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    BouncyButton(
                        onClick = {
                            haptics.tapSoft()
                            if (LocationHelper.hasLocationPermission(context)) {
                                onDetectLocation()
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_detect_gps_button"),
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (currentLocationName.isNotBlank()) {
                                "Perbarui GPS ($currentLocationName)"
                            } else {
                                "Deteksi Lokasi Otomatis (GPS)"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (currentLatitude != null && currentLongitude != null) {
                        BouncyTextButton(
                            onClick = {
                                haptics.tapSoft()
                                openInGoogleMaps(
                                    context = context,
                                    latitude = currentLatitude,
                                    longitude = currentLongitude,
                                    label = currentLocationName,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("settings_open_google_maps_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buka Koordinat di Google Maps")
                        }
                    }
                }
            }

            // Tema Hari Raya & Pratinjau
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Celebration,
                                contentDescription = null,
                                tint = atmo.accentCoral,
                            )
                            Column {
                                Text(
                                    text = "Tema Otomatis Hari Raya",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Ubah warna & ornamen pas hari libur nasional seperti Kemerdekaan, Idul Fitri, atau Imlek.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = holidayThemeEnabled,
                            onCheckedChange = {
                                haptics.tapSoft()
                                onHolidayThemeEnabledChange(it)
                            },
                            modifier = Modifier.testTag("settings_holiday_theme_switch"),
                        )
                    }

                    Text(
                        text = "Coba Pratinjau Tema Hari Raya",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HolidayThemeType.entries.forEach { themeOption ->
                            BouncyFilterChip(
                                selected = holidayThemePreview == themeOption,
                                onClick = {
                                    haptics.tapSoft()
                                    onHolidayThemePreviewChange(themeOption)
                                },
                                label = { Text(themeOption.labelId) },
                                modifier = Modifier.testTag("settings_holiday_preview_${themeOption.name}"),
                            )
                        }
                    }

                    if (holidayThemePreview != HolidayThemeType.NONE) {
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapConfirm()
                                onDismiss()
                                onReplayHolidayMotion()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("settings_replay_holiday_motion"),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Putar Animasi Motion Tema (${holidayThemePreview.labelId})")
                        }
                    }
                }
            }

            // Cadangan Data & Reset
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
                    Text(
                        text = if (isSimpleMode) "Simpan / Pindah Data Jadwal" else "Cadangan Data (JSON)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isSimpleMode) {
                            "Simpan semua jadwal dan foto kenanganmu ke satu file, atau pindahkan dari perangkat lain."
                        } else {
                            "Semua jadwal, sesi fokus, dan foto rekap tersimpan di perangkat. Ekspor kapan saja."
                        },
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
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("export_json_button"),
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSimpleMode) "Simpan File" else "Ekspor")
                        }
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                onImportJson()
                            },
                            enabled = !isBusyBackup,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("import_json_button"),
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSimpleMode) "Muat File" else "Impor")
                        }
                    }

                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapWarning()
                            showConfirmReset = true
                        },
                        enabled = !isBusyBackup,
                        shape = RoundedCornerShape(16.dp),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "JadwalNih v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("settings_version_label"),
                    )
                    BouncyTextButton(
                        onClick = {
                            haptics.tapSoft()
                            showPrivacyDialog = true
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_privacy_info_button"),
                    ) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Info Privasi")
                    }
                }
                BouncyTextButton(
                    onClick = {
                        haptics.tapSoft()
                        onDismiss()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionPill(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val interaction = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.16f) else atmo.cardAltBg,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "theme_pill_bg_$label",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "theme_pill_border_$label",
    )
    val fgColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "theme_pill_fg_$label",
    )

    Surface(
        modifier = modifier
            .bouncyPress(interaction, pressedScale = 0.94f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 1.8.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
