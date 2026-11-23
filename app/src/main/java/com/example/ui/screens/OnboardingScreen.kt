package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ThemeMode
import com.example.ui.components.BouncyButton
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.tapConfirm
import com.example.ui.components.tapSoft
import com.example.ui.theme.LocalAtmospherePalette

@Composable
fun OnboardingStartingScreen(
    initialName: String = "",
    initialSimpleMode: Boolean = false,
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onPreviewThemeMode: (ThemeMode) -> Unit,
    onComplete: (name: String, isSimpleMode: Boolean, themeMode: ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current

    var userName by remember(initialName) { mutableStateOf(initialName) }
    var isSimpleMode by remember(initialSimpleMode) { mutableStateOf(initialSimpleMode) }
    var themeMode by remember(initialThemeMode) { mutableStateOf(initialThemeMode) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(atmo.bgTop)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top Hero Branding
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_brand_icon),
                    contentDescription = "Ikon JadwalNih",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Text(
                    text = "Selamat Datang di JadwalNih!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Atur kegiatan harian, tambah jadwal otomatis pakai AI, dan simpan rekap fotomu dengan mudah.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = "JadwalNih v1.0 • Mulai dari ruang kosong yang rapi",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }

            // Feature Overview Card
            FrostedGlassCard(cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Kenalan Singkat Yuk",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OnboardingFeatureRow(
                        icon = Icons.Filled.CalendarToday,
                        tint = atmo.accentCobalt,
                        title = "Jadwal Harian & Pengingat",
                        description = "Deteksi jam bentrok otomatis plus konfirmasi kehadiran & pengingat tepat waktu.",
                    )
                    OnboardingFeatureRow(
                        icon = Icons.Filled.AutoAwesome,
                        tint = atmo.accentOrange,
                        title = "Tambah Otomatis Pakai AI",
                        description = "Salin prompt siap pakai, ceritakan kegiatanmu ke AI, lalu tempel hasilnya sekaligus.",
                    )
                    OnboardingFeatureRow(
                        icon = Icons.Filled.PhotoLibrary,
                        tint = atmo.accentMint,
                        title = "Rekap Foto & Fokus Timer",
                        description = "Simpan momen foto harian di kalender beserta catatan singkat dan pengatur fokus.",
                    )
                }
            }

            // Question 1: Nama Panggilan
            FrostedGlassCard(cornerRadius = 20.dp) {
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
                            text = "1. Siapa nama panggilanmu?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        placeholder = { Text("Tulis nama panggilanmu (opsional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input"),
                    )
                }
            }

            // Question 2: Mode Simpel vs Mode Normal
            FrostedGlassCard(cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "2. Pilih gaya tampilan yang nyaman",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OnboardingOptionCard(
                            title = "Mode Simpel",
                            subtitle = "Ringkas & santai. Hanya Jadwal, Fokus, dan Rekap Foto.",
                            icon = Icons.Filled.Spa,
                            accentColor = atmo.accentMint,
                            selected = isSimpleMode,
                            onClick = {
                                haptics.tapSoft()
                                isSimpleMode = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_mode_simpel"),
                        )
                        OnboardingOptionCard(
                            title = "Mode Normal",
                            subtitle = "Lengkap dengan Matriks Prioritas & Analitik.",
                            icon = Icons.Filled.GridView,
                            accentColor = atmo.accentCobalt,
                            selected = !isSimpleMode,
                            onClick = {
                                haptics.tapSoft()
                                isSimpleMode = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_mode_normal"),
                        )
                    }
                }
            }

            // Question 3: Mode Tema (Ikuti Sistem, Terang, Gelap)
            FrostedGlassCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "3. Pilih tema tampilan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OnboardingOptionCard(
                        title = "Ikuti Sistem (Bawaan)",
                        subtitle = "Otomatis menyesuaikan tema terang/gelap perangkat.",
                        icon = Icons.Filled.BrightnessAuto,
                        accentColor = atmo.accentCobalt,
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = {
                            haptics.tapSoft()
                            themeMode = ThemeMode.SYSTEM
                            onPreviewThemeMode(ThemeMode.SYSTEM)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_theme_system"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OnboardingOptionCard(
                            title = "Mode Terang",
                            subtitle = "Cerah bersih & segar.",
                            icon = Icons.Filled.LightMode,
                            accentColor = atmo.accentOrange,
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = {
                                haptics.tapSoft()
                                themeMode = ThemeMode.LIGHT
                                onPreviewThemeMode(ThemeMode.LIGHT)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_theme_light"),
                        )
                        OnboardingOptionCard(
                            title = "Mode Gelap",
                            subtitle = "Nyaman di mata saat malam.",
                            icon = Icons.Filled.DarkMode,
                            accentColor = atmo.accentAmber,
                            selected = themeMode == ThemeMode.DARK,
                            onClick = {
                                haptics.tapSoft()
                                themeMode = ThemeMode.DARK
                                onPreviewThemeMode(ThemeMode.DARK)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_theme_dark"),
                        )
                    }
                }
            }

            BouncyButton(
                onClick = {
                    haptics.tapConfirm()
                    onComplete(userName, isSimpleMode, themeMode)
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = atmo.accentCobalt,
                    contentColor = atmo.onAccentCobalt,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("onboarding_start_button"),
            ) {
                Text(
                    text = "Mulai Atur Jadwal",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = tint.copy(alpha = 0.14f),
            shape = CircleShape,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OnboardingOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val interaction = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.14f) else atmo.cardAltBg,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "onboard_opt_bg_$title",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "onboard_opt_border_$title",
    )

    Surface(
        modifier = modifier
            .bouncyPress(interaction, pressedScale = 0.95f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Terpilih",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
