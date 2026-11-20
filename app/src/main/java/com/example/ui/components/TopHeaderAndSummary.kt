package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.DateTimeHelper
import com.example.domain.FocusTimerState
import com.example.domain.QuickGlanceSummary
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.MonoSmallStyle
import java.io.File

@Composable
fun JadwalNihTopBar(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onOpenDataDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(atmo.heroGradStart, atmo.heroGradEnd),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "JN",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
            Column {
                Text(
                    text = "JadwalNih",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = DateTimeHelper.formatIndonesianFullDate(DateTimeHelper.todayIso()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = atmo.glassFillTop,
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(atmo.glassBorderStart, atmo.glassBorderEnd)),
                ),
            ) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("theme_toggle_button"),
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isDarkMode) "Ganti ke Mode Terang" else "Ganti ke Mode Gelap",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = atmo.glassFillTop,
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(atmo.glassBorderStart, atmo.glassBorderEnd)),
                ),
            ) {
                IconButton(
                    onClick = onOpenDataDialog,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("data_backup_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = "Cadangkan, Impor, atau Reset Data",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
fun RingkasanCepatCard(
    summary: QuickGlanceSummary,
    focusTimer: FocusTimerState,
    onOpenFocus: () -> Unit,
    onOpenRecap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val shape = RoundedCornerShape(22.dp)
    val completionFraction = if (summary.todayTotalCount == 0) {
        0f
    } else {
        summary.todayCompletedCount.toFloat() / summary.todayTotalCount.toFloat()
    }
    val completionPercent = (completionFraction * 100).toInt()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = atmo.heroGradStart.copy(alpha = 0.28f),
                spotColor = atmo.heroGradEnd.copy(alpha = 0.34f),
            ),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(atmo.heroGradStart, atmo.heroGradMid, atmo.heroGradEnd),
                        start = Offset.Zero,
                        end = Offset(900f, 500f),
                    ),
                )
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Hari Ini",
                            style = MaterialTheme.typography.labelMedium,
                            color = atmo.heroOnPrimary.copy(alpha = 0.85f),
                        )
                        Text(
                            text = summary.nextTaskTitle ?: "Hari ini beres!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = atmo.heroOnPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = atmo.heroSubtleBg,
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = summary.nextTaskTimeLabel ?: "${summary.todayActiveOrUpcomingCount} tersisa",
                                    style = MonoSmallStyle,
                                    color = atmo.heroOnPrimary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                )
                            }
                            if (summary.urgentOrCriticalCount > 0) {
                                Surface(
                                    color = Color(0x45FB7185),
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, Color(0x88FDA4AF)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = "Prioritas mendesak",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            text = "${summary.urgentOrCriticalCount} Mendesak",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Circular Progress Gauge
                    Box(
                        modifier = Modifier.size(78.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 7.dp.toPx()
                            drawArc(
                                color = Color.White.copy(alpha = 0.22f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke),
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFFBAE6FD), Color.White, Color(0xFFE9D5FF)),
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * completionFraction,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$completionPercent%",
                                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = "${summary.todayCompletedCount}/${summary.todayTotalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                // Bottom frosted tiles inside hero banner: Focus + Today's Recap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenFocus)
                            .testTag("quick_glance_focus_button"),
                        color = atmo.heroSubtleBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Buka Timer Fokus",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = when {
                                        focusTimer.isRunning -> "Fokus Aktif"
                                        focusTimer.isPaused -> "Dijeda"
                                        else -> "Fokus"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = if (focusTimer.hasActiveSession) {
                                        DateTimeHelper.formatCountdown(focusTimer.remainingSeconds)
                                    } else {
                                        "Siap ${focusTimer.plannedMinutes} mnt"
                                    },
                                    style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenRecap)
                            .testTag("quick_glance_recap_button"),
                        color = atmo.heroSubtleBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val thumbPath = summary.todayRecap?.imagePaths?.firstOrNull()
                            if (thumbPath != null && File(thumbPath).exists()) {
                                AsyncImage(
                                    model = File(thumbPath),
                                    contentDescription = "Thumbnail rekap hari ini",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = "Rekap foto hari ini",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Rekap",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = summary.todayRecap?.let {
                                        val count = it.imagePaths.size
                                        if (it.title.isNotBlank()) it.title else "$count foto"
                                    } ?: "Upload foto",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
