package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DateTimeHelper
import com.example.domain.ForeshadowOccurrence
import com.example.domain.HolidayInfo
import com.example.domain.HolidayThemeType
import com.example.domain.PersonalBirthdayStats
import com.example.domain.PrayerCardVisibilityMode
import com.example.domain.ReligionOption
import com.example.domain.TaskIconCatalog
import com.example.domain.WorshipActiveWindow
import com.example.domain.WorshipAnalyticsSummary
import com.example.domain.WorshipHistoryDay
import com.example.domain.WorshipScheduleItem
import com.example.ui.theme.HolidayHeaderOrnament
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle

@Composable
fun V1WelcomeCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current

    FrostedGlassCard(
        cornerRadius = 22.dp,
        borderColor = atmo.accentCobalt.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("v1_welcome_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Selamat datang di JadwalNih v1.0",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Versi siap rilis untuk temani harimu lebih tertata.",
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
                        .testTag("dismiss_welcome_card_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup kartu selamat datang",
                    )
                }
            }

            Text(
                text = "• Impor banyak jadwal AI sekaligus + 136 ikon keren\n" +
                    "• Pengingat sholat & ibadah sesuai agamamu + lokasi peta\n" +
                    "• Kalender terpadu, bayangan jadwal mingguan & tema Hari Raya",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HolidayGreetingBanner(
    holiday: HolidayInfo?,
    themeType: HolidayThemeType,
    modifier: Modifier = Modifier,
    onReplayMotion: (() -> Unit)? = null,
) {
    if (holiday == null && themeType == HolidayThemeType.NONE) return
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val resolvedTheme = if (themeType != HolidayThemeType.NONE) themeType else holiday?.themeType ?: HolidayThemeType.NONE
    val title = holiday?.name ?: "Tema ${resolvedTheme.labelId}"
    val greeting = holiday?.bannerGreeting ?: when (resolvedTheme) {
        HolidayThemeType.INDEPENDENCE_DAY -> "Dirgahayu Indonesia! Rayakan hari merdeka dengan semangat penuh."
        HolidayThemeType.IDUL_FITRI -> "Taqabbalallahu minna wa minkum. Selamat merayakan Hari Kemenangan!"
        HolidayThemeType.IMLEK -> "Gong Xi Fa Cai! Semoga tahun baru membawa keberkahan dan sukacita."
        HolidayThemeType.NATAL -> "Selamat Hari Natal! Semoga damai dan hangat selalu menyertaimu."
        HolidayThemeType.NYEPI_WAISAK -> "Selamat hari suci yang tenang dan penuh kebijaksanaan."
        HolidayThemeType.NONE -> ""
    }
    val isNationalObservance = holiday?.isNationalObservance == true
    val badgeBg = if (isNationalObservance) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
    val badgeFg = if (isNationalObservance) Color(0xFF92400E) else Color(0xFF991B1B)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = atmo.warmTintBg.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("holiday_greeting_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HolidayHeaderOrnament(themeType = resolvedTheme)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (holiday != null) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = if (isNationalObservance) "Hari Nasional" else "Libur Nasional",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (onReplayMotion != null && resolvedTheme != HolidayThemeType.NONE) {
                BouncyIconButton(
                    onClick = {
                        haptics.tapSoft()
                        onReplayMotion()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("replay_holiday_motion_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Putar animasi tema",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun BirthdayTodayMiniCard(
    userName: String,
    stats: PersonalBirthdayStats,
    onOpenCelebration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val displayName = userName.trim().ifBlank { "Sahabat JadwalNih" }

    Surface(
        onClick = {
            haptics.tapConfirm()
            onOpenCelebration()
        },
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFDF2F8).copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, Color(0xFFEC4899).copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("birthday_today_mini_card"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = Color(0xFFFBCFE8),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Cake,
                        contentDescription = "Selamat ulang tahun",
                        tint = Color(0xFFBE185D),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Selamat Ulang Tahun, $displayName!",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF831843),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Bersama JadwalNih ${stats.usageDurationLabel} • Ketuk untuk buka kartu ucapan",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9D174D),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorshipScheduleCard(
    religion: ReligionOption,
    items: List<WorshipScheduleItem>,
    nextItem: WorshipScheduleItem?,
    countdownLabel: String,
    locationName: String,
    onToggleComplete: (WorshipScheduleItem) -> Unit,
    modifier: Modifier = Modifier,
    visibilityMode: PrayerCardVisibilityMode = PrayerCardVisibilityMode.ONLY_ACTIVE_WINDOW,
    activeWindow: WorshipActiveWindow = WorshipActiveWindow(),
    onOpenHistory: () -> Unit = {},
) {
    if (items.isEmpty()) return
    // Strict rule: hide completely outside active worship window when in ONLY_ACTIVE_WINDOW mode
    if (visibilityMode == PrayerCardVisibilityMode.ONLY_ACTIVE_WINDOW && !activeWindow.isInActiveWindow) {
        return
    }

    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val statusPalette = LocalStatusPalette.current
    val haptics = LocalHapticFeedback.current
    var expanded by remember(visibilityMode) {
        mutableStateOf(visibilityMode == PrayerCardVisibilityMode.ALWAYS_SHOW)
    }

    val doneCount = items.count { it.isCompleted }

    if (visibilityMode == PrayerCardVisibilityMode.ONLY_ACTIVE_WINDOW) {
        val activeItem = activeWindow.activeItem ?: return
        FrostedGlassCard(
            cornerRadius = 22.dp,
            borderColor = atmo.accentMint.copy(alpha = 0.72f),
            modifier = modifier
                .fillMaxWidth()
                .testTag("worship_schedule_card"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spec.cardPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = statusPalette.greenBg,
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Mosque,
                                contentDescription = "Waktu ibadah aktif",
                                tint = statusPalette.greenFg,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Waktu ${activeItem.title} (${activeItem.formattedTime})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Surface(
                                color = statusPalette.greenBg,
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = "$doneCount/${items.size}",
                                    style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                    color = statusPalette.greenFg,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                        val windowLabel = buildString {
                            append("Aktif s.d. ${DateTimeHelper.formatMinuteOfDay(activeWindow.windowEndMinute)}")
                            if (locationName.isNotBlank()) {
                                append(" • $locationName")
                            }
                        }
                        Text(
                            text = windowLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            onOpenHistory()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("worship_open_history_button"),
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Riwayat 7 Hari", style = MaterialTheme.typography.labelMedium)
                    }

                    BouncyButton(
                        onClick = {
                            haptics.tapConfirm()
                            onToggleComplete(activeItem)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeItem.isCompleted) statusPalette.greenBg else atmo.accentMint,
                            contentColor = if (activeItem.isCompleted) statusPalette.greenFg else atmo.onAccentMint,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("worship_quick_check_next"),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (activeItem.isCompleted) "Sudah ${activeItem.title} ✓" else "Sudah ${activeItem.title}",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        return
    }

    FrostedGlassCard(
        cornerRadius = 22.dp,
        borderColor = atmo.accentMint.copy(alpha = 0.6f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("worship_schedule_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spec.cardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptics.tapSoft()
                        expanded = !expanded
                    },
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
                            imageVector = Icons.Filled.Mosque,
                            contentDescription = "Jadwal ibadah",
                            tint = statusPalette.greenFg,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (religion == ReligionOption.ISLAM) "Waktu Sholat" else "Ibadah ${religion.labelId}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "$doneCount/${items.size}",
                                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val currentActiveItem = activeWindow.currentItem
                        Text(
                            text = when {
                                activeWindow.isWithinNaturalWindow && currentActiveItem != null ->
                                    "Aktif: ${currentActiveItem.title} s.d. ${DateTimeHelper.formatMinuteOfDay(activeWindow.windowEndMinute)}"
                                nextItem != null -> "${nextItem.title} (${nextItem.formattedTime}) • $countdownLabel"
                                else -> "Semua ibadah hari ini sudah beres!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (locationName.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = locationName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Ringkas daftar ibadah" else "Lihat semua waktu ibadah",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BouncyOutlinedButton(
                    onClick = {
                        haptics.tapSoft()
                        onOpenHistory()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("worship_open_history_button"),
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Riwayat 7 Hari", style = MaterialTheme.typography.labelMedium)
                }

                val targetButtonItem = activeWindow.activeItem ?: nextItem
                if (targetButtonItem != null) {
                    BouncyButton(
                        onClick = {
                            haptics.tapConfirm()
                            onToggleComplete(targetButtonItem)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = atmo.accentMint,
                            contentColor = atmo.onAccentMint,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("worship_quick_check_next"),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sudah ${targetButtonItem.title}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = expanded || nextItem == null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (item.isCompleted) {
                                statusPalette.greenBg.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptics.tapConfirm()
                                    onToggleComplete(item)
                                }
                                .minimumInteractiveComponentSize()
                                .testTag("worship_item_${item.key}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = {
                                        haptics.tapConfirm()
                                        onToggleComplete(item)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = statusPalette.greenAccent,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (item.subtitle.isNotBlank()) {
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text(
                                    text = item.formattedTime,
                                    style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.isCompleted) statusPalette.greenFg else MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorshipHistorySheet(
    religion: ReligionOption,
    historyDays: List<WorshipHistoryDay>,
    allowManualMissedEdit: Boolean,
    onToggleItem: (WorshipScheduleItem) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }

    val atmo = LocalAtmospherePalette.current
    val statusPalette = LocalStatusPalette.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = atmo.sheetBg,
        modifier = Modifier.testTag("worship_history_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (religion == ReligionOption.ISLAM) "Riwayat Sholat (7 Hari)" else "Riwayat Ibadah (7 Hari)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Centang ibadah hari ini atau ubah manual 7 hari terakhir yang terlewat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BouncyIconButton(
                    onClick = {
                        haptics.tapSoft()
                        onDismiss()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup riwayat ibadah")
                }
            }

            if (!allowManualMissedEdit) {
                Surface(
                    color = statusPalette.yellowBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, statusPalette.yellowAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("worship_history_locked_banner"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Edit manual terkunci",
                            tint = statusPalette.yellowFg,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Edit sholat lewat dikunci",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = statusPalette.yellowFg,
                            )
                            Text(
                                text = "Hari lalu hanya baca. Aktifkan di Pengaturan jika ingin mengubah manual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusPalette.yellowFg,
                            )
                        }
                        BouncyTextButton(
                            onClick = {
                                haptics.tapSoft()
                                onDismiss()
                                onOpenSettings()
                            },
                        ) {
                            Text("Pengaturan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            historyDays.forEach { day ->
                val dayEditable = day.isToday || allowManualMissedEdit
                val doneCount = day.items.count { it.isCompleted }
                Surface(
                    color = atmo.sheetCardBg,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        if (day.isToday) atmo.accentMint.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.testTag("worship_history_day_${day.dateIso}"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (day.isToday) {
                                    "Hari Ini • ${DateTimeHelper.formatShortDate(day.dateIso)}"
                                } else {
                                    DateTimeHelper.formatIndonesianFullDate(day.dateIso)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (!dayEditable) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Terkunci",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Surface(
                                    color = if (doneCount == day.items.size && day.items.isNotEmpty()) {
                                        statusPalette.greenBg
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                ) {
                                    Text(
                                        text = "$doneCount/${day.items.size}",
                                        style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                        color = if (doneCount == day.items.size && day.items.isNotEmpty()) {
                                            statusPalette.greenFg
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        day.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = dayEditable) {
                                        haptics.tapConfirm()
                                        onToggleItem(item)
                                    }
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .alpha(if (dayEditable) 1f else 0.72f)
                                    .testTag("worship_history_item_${day.dateIso}_${item.key}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = {
                                        haptics.tapConfirm()
                                        onToggleItem(item)
                                    },
                                    enabled = dayEditable,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = statusPalette.greenAccent,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = item.formattedTime,
                                    style = MonoSmallStyle,
                                    color = if (item.isCompleted) statusPalette.greenFg else MaterialTheme.colorScheme.onSurfaceVariant,
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
fun WeeklyWorshipAnalyticsCard(
    summary: WorshipAnalyticsSummary,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val statusPalette = LocalStatusPalette.current
    FrostedGlassCard(
        cornerRadius = 22.dp,
        borderColor = atmo.accentMint.copy(alpha = 0.55f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_worship_analytics_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mosque,
                        contentDescription = null,
                        tint = statusPalette.greenFg,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Konsistensi Ibadah Minggu Ini",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "${summary.weeklyCompletionRatePercent}%",
                    style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                    color = statusPalette.greenFg,
                )
            }

            if (!summary.worshipEnabled) {
                Text(
                    text = "Pengingat ibadah sedang dimatikan. Kamu bisa aktifkan lagi kapan saja di Pengaturan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinearProgressIndicator(
                    progress = { (summary.weeklyCompletionRatePercent / 100f).coerceIn(0f, 1f) },
                    color = statusPalette.greenAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp)),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Hari ini: ${summary.todayCompletedCount}/${summary.todayTotalCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "7 hari: ${summary.weeklyCompletedCount}/${summary.weeklyTotalCount} (${summary.religion.labelId})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ForeshadowGhostCard(
    occurrence: ForeshadowOccurrence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val task = occurrence.parentTask
    val resolvedIcon = TaskIconCatalog.resolveSpec(task.iconKey, task.title)
    val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.76f)
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                haptics.tapSoft()
                onClick()
            }
            .minimumInteractiveComponentSize()
            .testTag("foreshadow_card_${task.id}_${occurrence.occurrenceDateIso}"),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = dashColor,
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                ),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = resolvedIcon.icon,
                        contentDescription = resolvedIcon.labelId,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "Bayangan M+${occurrence.weeksAhead}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "${DateTimeHelper.formatIndonesianFullDate(occurrence.occurrenceDateIso)} • ${
                        DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay)
                    }",
                    style = MonoSmallStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Circle,
                contentDescription = "Lihat opsi jadwal bayangan",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForeshadowActionSheet(
    occurrence: ForeshadowOccurrence,
    onActivateEarly: () -> Unit,
    onSkipWeek: () -> Unit,
    onDismiss: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val task = occurrence.parentTask
    val resolvedIcon = TaskIconCatalog.resolveSpec(task.iconKey, task.title)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = atmo.sheetBg,
        modifier = Modifier.testTag("foreshadow_action_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = resolvedIcon.icon,
                            contentDescription = resolvedIcon.labelId,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Pratinjau berulang ${occurrence.weeksAhead} minggu lagi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            FrostedGlassCard(cornerRadius = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${DateTimeHelper.formatIndonesianFullDate(occurrence.occurrenceDateIso)} • ${
                                DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (task.locationName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(text = task.locationName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (task.notes.isNotBlank()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Jadwal ini otomatis aktif saat minggu itu tiba. Kamu bisa mengeditnya lebih awal atau melewatinya.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            BouncyButton(
                onClick = {
                    haptics.tapConfirm()
                    onActivateEarly()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = atmo.accentCobalt,
                    contentColor = atmo.onAccentCobalt,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("foreshadow_activate_button"),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Aktifkan & Edit Lebih Awal", fontWeight = FontWeight.Bold)
            }

            BouncyOutlinedButton(
                onClick = {
                    haptics.tapSoft()
                    onSkipWeek()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("foreshadow_skip_button"),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lewati Minggu Ini")
            }
        }
    }
}
