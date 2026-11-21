package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoRecap
import com.example.domain.PhotoTextAlignment
import com.example.domain.PhotoTextStyle
import com.example.domain.TaskWithStatus
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.LocalStatusPalette
import com.example.ui.theme.MonoSmallStyle
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostCapturePreviewSheet(
    imageUri: Uri,
    targetDateIso: String,
    existingTitle: String,
    existingCaption: String,
    timeLabel: String,
    onDismiss: () -> Unit,
    onConfirmSave: (
        textStyle: PhotoTextStyle,
        textAlignment: PhotoTextAlignment,
        title: String,
        caption: String,
    ) -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTextStyle by remember { mutableStateOf(PhotoTextStyle.TIME_PILL) }
    var selectedTextAlignment by remember { mutableStateOf(PhotoTextAlignment.CENTER) }
    var titleInput by remember(existingTitle) { mutableStateOf(existingTitle) }
    var captionInput by remember(existingCaption) { mutableStateOf(existingCaption) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = atmo.sheetBg,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = atmo.sheetScrim,
        tonalElevation = 0.dp,
        dragHandle = { FrostedSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedSheetBackdrop()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Pratinjau Momen",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Masuk ke ${DateTimeHelper.formatIndonesianFullDate(targetDateIso)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Photo preview frame with live text style & alignment overlay (no color filters)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        atmo.cardBorder,
                        RoundedCornerShape(24.dp),
                    ),
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Pratinjau foto rekap",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                val badgeAlignment = when (selectedTextAlignment) {
                    PhotoTextAlignment.LEFT -> Alignment.BottomStart
                    PhotoTextAlignment.CENTER -> Alignment.BottomCenter
                    PhotoTextAlignment.RIGHT -> Alignment.BottomEnd
                }
                val columnAlignment = when (selectedTextAlignment) {
                    PhotoTextAlignment.LEFT -> Alignment.Start
                    PhotoTextAlignment.CENTER -> Alignment.CenterHorizontally
                    PhotoTextAlignment.RIGHT -> Alignment.End
                }
                val textAlign = when (selectedTextAlignment) {
                    PhotoTextAlignment.LEFT -> TextAlign.Start
                    PhotoTextAlignment.CENTER -> TextAlign.Center
                    PhotoTextAlignment.RIGHT -> TextAlign.End
                }

                when (selectedTextStyle) {
                    PhotoTextStyle.TIME_PILL -> {
                        Surface(
                            color = Color(0xB8080D21),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .align(badgeAlignment)
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFBAE6FD),
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = timeLabel,
                                    style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    PhotoTextStyle.CAPTION_BOTTOM -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color(0xC2070B19))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = columnAlignment,
                            ) {
                                Text(
                                    text = titleInput.trim().ifBlank { "Momen Hari Ini" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = textAlign,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = timeLabel,
                                    style = MonoSmallStyle,
                                    color = Color(0xFFBAE6FD),
                                    textAlign = textAlign,
                                )
                            }
                        }
                    }

                    PhotoTextStyle.NONE -> Unit
                }
            }

            // Text Style Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Gaya Teks di Foto",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoTextStyle.entries.forEach { style ->
                        BouncyFilterChip(
                            selected = selectedTextStyle == style,
                            onClick = {
                                haptics.tapSoft()
                                selectedTextStyle = style
                            },
                            label = { Text(style.label) },
                        )
                    }
                }
            }

            // Text Layout Alignment selector (Kiri, Tengah, Kanan)
            AnimatedVisibility(
                visible = selectedTextStyle != PhotoTextStyle.NONE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Posisi Teks",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PhotoTextAlignment.entries.forEach { alignment ->
                            val icon = when (alignment) {
                                PhotoTextAlignment.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                PhotoTextAlignment.CENTER -> Icons.Filled.FormatAlignCenter
                                PhotoTextAlignment.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                            }
                            BouncyFilterChip(
                                selected = selectedTextAlignment == alignment,
                                onClick = {
                                    haptics.tapSoft()
                                    selectedTextAlignment = alignment
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = { Text(alignment.label) },
                                modifier = Modifier.testTag("overlay_align_${alignment.name.lowercase()}"),
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Judul Singkat (Opsional)") },
                placeholder = { Text("Misal: Kopi & belajar sore") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_recap_title_input"),
            )

            OutlinedTextField(
                value = captionInput,
                onValueChange = { captionInput = it },
                label = { Text("Catatan Singkat (Opsional)") },
                placeholder = { Text("Cerita singkat momen ini...") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_recap_caption_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BouncyOutlinedButton(
                    onClick = {
                        haptics.tapSoft()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(0.42f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize(),
                ) {
                    Text("Ulangi")
                }

                BouncyButton(
                    onClick = {
                        haptics.tapConfirm()
                        onConfirmSave(selectedTextStyle, selectedTextAlignment, titleInput, captionInput)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = atmo.accentMint,
                        contentColor = atmo.onAccentMint,
                    ),
                    modifier = Modifier
                        .weight(0.58f)
                        .height(48.dp)
                        .testTag("confirm_save_captured_recap_button"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan ke Rekap")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecapCalendarSheet(
    selectedDateIso: String,
    photoRecaps: Map<String, PhotoRecap>,
    allTasks: List<TaskWithStatus> = emptyList(),
    isExportingCollage: Boolean = false,
    onSelectDate: (String) -> Unit,
    onJumpToCameraForDate: (String) -> Unit,
    onPickGalleryForDate: (String) -> Unit,
    onPickFileForDate: (String) -> Unit,
    onRemovePhoto: (String, String) -> Unit,
    onSaveMeta: (String, String, String) -> Unit,
    onDeleteRecap: (String) -> Unit,
    onExportCollage: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val statusPalette = LocalStatusPalette.current
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val todayIso = remember { DateTimeHelper.todayIso() }

    var visibleMonthAnchor by remember(selectedDateIso) { mutableStateOf(selectedDateIso) }
    var zoomedImagePath by remember { mutableStateOf<String?>(null) }
    var pendingDeleteDate by remember { mutableStateOf<String?>(null) }

    val activeRecap = photoRecaps[selectedDateIso]
    val photos = activeRecap?.imagePaths.orEmpty()
    val dayTasks = remember(allTasks, selectedDateIso) {
        allTasks.filter { it.task.dateIso == selectedDateIso }.sortedBy { it.task.startMinuteOfDay }
    }

    var titleInput by remember(selectedDateIso, activeRecap?.title) {
        mutableStateOf(activeRecap?.title ?: "")
    }
    var captionInput by remember(selectedDateIso, activeRecap?.caption) {
        mutableStateOf(activeRecap?.caption ?: "")
    }

    LaunchedEffect(selectedDateIso, activeRecap) {
        titleInput = activeRecap?.title ?: ""
        captionInput = activeRecap?.caption ?: ""
    }

    zoomedImagePath?.let { path ->
        Dialog(onDismissRequest = { zoomedImagePath = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Pratinjau foto penuh",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                    )
                    BouncyIconButton(
                        onClick = {
                            haptics.tapSoft()
                            zoomedImagePath = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup pratinjau",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }

    pendingDeleteDate?.let { dateToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteDate = null },
            containerColor = atmo.sheetBg,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Hapus Rekap?") },
            text = {
                Text(
                    "Foto dan catatan rekap untuk ${DateTimeHelper.formatIndonesianFullDate(dateToDelete)} akan dihapus permanen.",
                )
            },
            confirmButton = {
                BouncyButton(
                    onClick = {
                        haptics.tapWarning()
                        onDeleteRecap(dateToDelete)
                        pendingDeleteDate = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.testTag("confirm_delete_recap_button"),
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                BouncyTextButton(
                    onClick = {
                        haptics.tapSoft()
                        pendingDeleteDate = null
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
        dragHandle = { FrostedSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedSheetBackdrop()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Calendar Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Kalender Rekap",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Sinkron dengan Matriks Bulanan. Ketuk tanggal untuk detail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selectedDateIso != todayIso || visibleMonthAnchor.take(7) != todayIso.take(7)) {
                    BouncyTextButton(
                        onClick = {
                            haptics.tapSoft()
                            visibleMonthAnchor = todayIso
                            onSelectDate(todayIso)
                        },
                    ) {
                        Text("Hari Ini")
                    }
                }
            }

            Surface(
                color = atmo.sheetCardBg,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BouncyIconButton(
                            onClick = {
                                haptics.tapTick()
                                visibleMonthAnchor = DateTimeHelper.addMonths(visibleMonthAnchor, -1)
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                        }
                        Text(
                            text = DateTimeHelper.formatMonthYear(visibleMonthAnchor),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        BouncyIconButton(
                            onClick = {
                                haptics.tapTick()
                                visibleMonthAnchor = DateTimeHelper.addMonths(visibleMonthAnchor, 1)
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                        }
                    }

                    // Weekday headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // 7-column photo-thumbnail + schedule indicator calendar grid
                    val cells = remember(visibleMonthAnchor) {
                        DateTimeHelper.monthGridCells(visibleMonthAnchor)
                    }
                    cells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            week.forEach { cell ->
                                if (cell.dateIso == null || cell.dayOfMonth == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.88f),
                                    )
                                } else {
                                    val cellDate = cell.dateIso
                                    val recap = photoRecaps[cellDate]
                                    val thumbPath = recap?.imagePaths?.firstOrNull()?.takeIf { File(it).exists() }
                                    val cellTasks = allTasks.filter { it.task.dateIso == cellDate }
                                    val taskDotColor = cellTasks.firstOrNull { !it.task.isCompleted }?.let {
                                        statusPalette.colorsFor(it.status).accent
                                    } ?: if (cellTasks.isNotEmpty()) statusPalette.greenAccent else null
                                    val isSelected = cellDate == selectedDateIso
                                    val isToday = cellDate == todayIso
                                    val shape = RoundedCornerShape(12.dp)

                                    val dayInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.88f)
                                            .bouncyPress(dayInteraction, pressedScale = 0.90f)
                                            .clip(shape)
                                            .background(
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                                                },
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 1.dp,
                                                color = when {
                                                    isSelected -> atmo.accentMint
                                                    isToday -> atmo.accentCobalt
                                                    else -> MaterialTheme.colorScheme.outlineVariant
                                                },
                                                shape = shape,
                                            )
                                            .clickable(
                                                interactionSource = dayInteraction,
                                                indication = null,
                                            ) {
                                                haptics.tapSoft()
                                                onSelectDate(cellDate)
                                            }
                                            .testTag("calendar_day_${cellDate}"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (thumbPath != null) {
                                            AsyncImage(
                                                model = File(thumbPath),
                                                contentDescription = "Foto rekap ${cell.dayOfMonth}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.22f)),
                                            )
                                            Surface(
                                                color = Color(0xCC0E121B),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp),
                                            ) {
                                                Text(
                                                    text = cell.dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                )
                                            }
                                            val count = recap?.imagePaths?.size ?: 0
                                            if (count > 1) {
                                                Surface(
                                                    color = atmo.accentCobalt,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(4.dp),
                                                ) {
                                                    Text(
                                                        text = "$count/4",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = atmo.onAccentCobalt,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                            ) {
                                                Text(
                                                    text = cell.dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    ),
                                                    color = when {
                                                        isSelected -> atmo.accentMint
                                                        isToday -> atmo.accentCobalt
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                )
                                                if (recap != null && (recap.title.isNotBlank() || recap.caption.isNotBlank())) {
                                                    Spacer(Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(atmo.accentAmber),
                                                    )
                                                }
                                            }
                                        }

                                        if (taskDotColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(taskDotColor),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Recap Detail Panel
            Surface(
                color = atmo.sheetCardBg,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DateTimeHelper.formatIndonesianFullDate(selectedDateIso),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Foto (${photos.size}/4) • ${dayTasks.size} jadwal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (photos.size < 4) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onJumpToCameraForDate(selectedDateIso)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Jepret")
                            }
                        }
                    }

                    if (photos.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onPickGalleryForDate(selectedDateIso)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Galeri")
                            }
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onPickFileForDate(selectedDateIso)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("File")
                            }
                        }
                    } else {
                        AutoPhotoCollage(
                            paths = photos,
                            onZoom = { zoomedImagePath = it },
                            onRemove = { onRemovePhoto(selectedDateIso, it) },
                        )

                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapConfirm()
                                onExportCollage(selectedDateIso)
                            },
                            enabled = !isExportingCollage,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, atmo.accentMint.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = atmo.accentMint,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_collage_button"),
                        ) {
                            if (isExportingCollage) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Membuat Kolase...")
                            } else {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ekspor Kolase ke Galeri & Bagikan")
                            }
                        }
                    }

                    // Synced Schedule Summary for the tapped date
                    if (dayTasks.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("recap_synced_tasks_card"),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Jadwal di Tanggal Ini (${dayTasks.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                dayTasks.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.task.title,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = DateTimeHelper.formatTimeRange(
                                                    item.task.startMinuteOfDay,
                                                    item.task.endMinuteOfDay,
                                                ),
                                                style = MonoSmallStyle,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            QuadrantChip(quadrant = item.task.quadrant)
                                            StatusBadge(status = item.status)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pastel frosted purple-blue Spiral Mini-Notebook Card with text-adaptive ruled lines
                    SpiralNotebookCard(
                        title = titleInput,
                        onTitleChange = { titleInput = it },
                        caption = captionInput,
                        onCaptionChange = { captionInput = it },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (activeRecap != null) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapWarning()
                                    pendingDeleteDate = selectedDateIso
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_recap_button"),
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus rekap", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Hapus")
                            }
                        }

                        BouncyButton(
                            onClick = {
                                haptics.tapConfirm()
                                onSaveMeta(selectedDateIso, titleInput, captionInput)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = atmo.accentCobalt,
                                contentColor = atmo.onAccentCobalt,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_recap_meta_button"),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Simpan Catatan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpiralNotebookCard(
    title: String,
    onTitleChange: (String) -> Unit,
    caption: String,
    onCaptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val isDark = atmo.isDark

    val headerBgColor = if (isDark) Color(0xFF28334E) else Color(0xFFE0ECFF)
    val pageBgColor = if (isDark) Color(0xFF1F273B) else Color(0xFFF6FAFF)
    val lipBgColor = if (isDark) Color(0xFF161D2E) else Color(0xFFD6E4FF)
    val borderColor = if (isDark) Color(0xFF4F6089) else Color(0xFFB3CDFF)
    val ruledLineColor = if (isDark) Color(0x3860A5FA) else Color(0x381677FF)
    val badgeBgColor = if (isDark) atmo.accentAmber else atmo.accentCobalt
    val badgeFgColor = if (isDark) atmo.onAccentAmber else Color.White
    val ringColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1677FF)
    val ringSheenColor = if (isDark) Color(0xFFDBEAFE) else Color(0xFFEFF6FF)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827)
    val hintColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val holeBgColor = if (isDark) Color(0xFF0E121B) else Color(0xFFCBD5E1)

    var captionTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        // Subtle stacked-page lip underneath the bottom edge
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 14.dp, start = 4.dp, end = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(lipBgColor)
                .border(1.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
        )

        // Main solid pastel notebook rectangle (no gradient)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(pageBgColor)
                .border(
                    width = 1.2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp),
                ),
        ) {
            // Tinted notebook header band (with top clearance for spiral punch holes) for short title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    color = badgeBgColor,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = "JUDUL CATATAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = badgeFgColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }

                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    ),
                    cursorBrush = SolidColor(ringColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recap_title_input"),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Tulis judul singkat hari ini...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = hintColor,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = borderColor,
            )

            // Rectangular ruled-paper note body: lines adapt dynamically to measured TextLayoutResult lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 148.dp)
                    .drawBehind {
                        val topPadPx = 14.dp.toPx()
                        val layout = captionTextLayout
                        val measuredLineHeight = if (layout != null && layout.lineCount > 0) {
                            (layout.getLineBottom(0) - layout.getLineTop(0)).coerceAtLeast(18.dp.toPx())
                        } else {
                            24.sp.toPx()
                        }
                        var lastLineY = topPadPx
                        if (layout != null && layout.lineCount > 0) {
                            for (lineIdx in 0 until layout.lineCount) {
                                val y = topPadPx + layout.getLineBottom(lineIdx)
                                drawLine(
                                    color = ruledLineColor,
                                    start = Offset(14.dp.toPx(), y),
                                    end = Offset(size.width - 14.dp.toPx(), y),
                                    strokeWidth = 1.dp.toPx(),
                                )
                                lastLineY = y
                            }
                        } else {
                            val firstY = topPadPx + measuredLineHeight
                            drawLine(
                                color = ruledLineColor,
                                start = Offset(14.dp.toPx(), firstY),
                                end = Offset(size.width - 14.dp.toPx(), firstY),
                                strokeWidth = 1.dp.toPx(),
                            )
                            lastLineY = firstY
                        }

                        var extraY = lastLineY + measuredLineHeight
                        while (extraY < size.height - 10.dp.toPx()) {
                            drawLine(
                                color = ruledLineColor,
                                start = Offset(14.dp.toPx(), extraY),
                                end = Offset(size.width - 14.dp.toPx(), extraY),
                                strokeWidth = 1.dp.toPx(),
                            )
                            extraY += measuredLineHeight
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    onTextLayout = { captionTextLayout = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        color = textColor,
                    ),
                    cursorBrush = SolidColor(ringColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp)
                        .testTag("recap_caption_input"),
                    decorationBox = { innerTextField ->
                        Box {
                            if (caption.isEmpty()) {
                                Text(
                                    text = "Tulis catatan singkatmu di sini...\nCeritakan highlight atau progres hari ini.",
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                                    color = hintColor,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        // Protruding top spiral binder rings + punched holes
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .align(Alignment.TopCenter),
        ) {
            val ringCount = 7
            val horizontalPad = 28.dp.toPx()
            val usableWidth = (size.width - horizontalPad * 2f).coerceAtLeast(size.width * 0.5f)
            val step = usableWidth / (ringCount - 1).coerceAtLeast(1)
            val holeY = 17.dp.toPx()
            val holeRadius = 4.8.dp.toPx()
            val loopTopY = 2.dp.toPx()
            val loopHalfWidth = 5.2.dp.toPx()

            for (i in 0 until ringCount) {
                val cx = horizontalPad + i * step
                drawCircle(
                    color = holeBgColor,
                    radius = holeRadius,
                    center = Offset(cx, holeY),
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.55f),
                    radius = holeRadius,
                    center = Offset(cx, holeY),
                    style = Stroke(width = 1.2.dp.toPx()),
                )

                val wirePath = Path().apply {
                    moveTo(cx - loopHalfWidth * 0.55f, holeY)
                    cubicTo(
                        cx - loopHalfWidth * 1.15f,
                        loopTopY,
                        cx + loopHalfWidth * 1.15f,
                        loopTopY,
                        cx + loopHalfWidth * 0.55f,
                        holeY - 1.dp.toPx(),
                    )
                }
                drawPath(
                    path = wirePath,
                    color = ringColor,
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
                )
                drawPath(
                    path = wirePath,
                    color = ringSheenColor.copy(alpha = 0.72f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun FrostedSheetDragHandle() {
    val atmo = LocalAtmospherePalette.current
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
}

@Composable
private fun Modifier.frostedSheetBackdrop(): Modifier {
    val atmo = LocalAtmospherePalette.current
    return background(atmo.sheetBg)
}

@Composable
fun AutoPhotoCollage(
    paths: List<String>,
    onZoom: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    when (paths.size) {
        1 -> {
            CollageImageTile(
                path = paths[0],
                onZoom = { onZoom(paths[0]) },
                onRemove = { onRemove(paths[0]) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f),
            )
        }
        2 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                paths.forEach { p ->
                    CollageImageTile(
                        path = p,
                        onZoom = { onZoom(p) },
                        onRemove = { onRemove(p) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        3 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CollageImageTile(
                    path = paths[0],
                    onZoom = { onZoom(paths[0]) },
                    onRemove = { onRemove(paths[0]) },
                    modifier = Modifier.weight(1.25f).fillMaxHeight(),
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CollageImageTile(
                        path = paths[1],
                        onZoom = { onZoom(paths[1]) },
                        onRemove = { onRemove(paths[1]) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    CollageImageTile(
                        path = paths[2],
                        onZoom = { onZoom(paths[2]) },
                        onRemove = { onRemove(paths[2]) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                paths.take(4).chunked(2).forEach { rowPaths ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowPaths.forEach { p ->
                            CollageImageTile(
                                path = p,
                                onZoom = { onZoom(p) },
                                onRemove = { onRemove(p) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageImageTile(
    path: String,
    onZoom: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val removeInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptics.tapSoft()
                onZoom()
            },
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = "Foto rekap harian",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            onClick = {
                haptics.tapSoft()
                onRemove()
            },
            interactionSource = removeInteraction,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .bouncyPress(removeInteraction, pressedScale = 0.85f)
                .minimumInteractiveComponentSize(),
        ) {
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Hapus foto",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
