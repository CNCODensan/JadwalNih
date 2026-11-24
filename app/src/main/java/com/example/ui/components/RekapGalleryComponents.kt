package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoRecap
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.MonoSmallStyle
import java.io.File

private enum class RekapGalleryFilter(val label: String) {
    ALL("Semua Momen"),
    TODAY("Hari Ini"),
    SELECTED("Tanggal Dipilih"),
}

private data class GalleryLightboxSelection(
    val dateIso: String,
    val path: String,
    val title: String,
    val caption: String,
    val locationName: String = "",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RekapGalleryView(
    photoRecaps: Map<String, PhotoRecap>,
    selectedDateIso: String,
    todayIso: String,
    isExportingCollage: Boolean,
    onOpenCameraForDate: (String) -> Unit,
    onPickGalleryForDate: (String) -> Unit,
    onRemovePhoto: (String, String) -> Unit,
    onExportCollage: (String) -> Unit,
    onOpenCalendarForDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    var activeFilter by remember { mutableStateOf(RekapGalleryFilter.ALL) }
    var lightboxItem by remember { mutableStateOf<GalleryLightboxSelection?>(null) }
    var pendingDeletePhoto by remember { mutableStateOf<Pair<String, String>?>(null) }

    val allPhotoDays = remember(photoRecaps) {
        photoRecaps.values
            .mapNotNull { recap ->
                val existingPaths = recap.imagePaths.filter { it.isNotBlank() && File(it).exists() }
                if (existingPaths.isEmpty()) {
                    null
                } else {
                    recap.copy(imagePaths = existingPaths)
                }
            }
            .sortedByDescending { it.dateIso }
    }

    val totalPhotosCount = remember(allPhotoDays) {
        allPhotoDays.sumOf { it.imagePaths.size }
    }

    val displayedDays = remember(allPhotoDays, activeFilter, todayIso, selectedDateIso) {
        when (activeFilter) {
            RekapGalleryFilter.ALL -> allPhotoDays
            RekapGalleryFilter.TODAY -> allPhotoDays.filter { it.dateIso == todayIso }
            RekapGalleryFilter.SELECTED -> allPhotoDays.filter { it.dateIso == selectedDateIso }
        }
    }

    lightboxItem?.let { item ->
        Dialog(
            onDismissRequest = { lightboxItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = atmo.sheetBg,
                border = BorderStroke(1.dp, atmo.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .testTag("gallery_lightbox_dialog"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DateTimeHelper.formatIndonesianFullDate(item.dateIso),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            if (item.title.isNotBlank()) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = atmo.accentMint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (item.locationName.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .testTag("gallery_lightbox_location"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Lokasi momen",
                                        tint = atmo.accentMint,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = item.locationName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        BouncyIconButton(
                            onClick = {
                                haptics.tapSoft()
                                lightboxItem = null
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("gallery_lightbox_close"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Tutup pratinjau foto",
                                tint = Color.White,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = File(item.path),
                            contentDescription = "Pratinjau foto rekap",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    if (item.caption.isNotBlank()) {
                        Text(
                            text = item.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapWarning()
                                pendingDeletePhoto = item.dateIso to item.path
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, atmo.accentCoral.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = atmo.accentCoral),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("gallery_lightbox_delete"),
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hapus")
                        }
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                lightboxItem = null
                                onOpenCalendarForDate(item.dateIso)
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, atmo.cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Kalender")
                        }
                        BouncyButton(
                            onClick = {
                                haptics.tapConfirm()
                                onExportCollage(item.dateIso)
                            },
                            enabled = !isExportingCollage,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = atmo.accentMint,
                                contentColor = atmo.onAccentMint,
                            ),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        ) {
                            if (isExportingCollage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = atmo.onAccentMint,
                                )
                            } else {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Kolase")
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeletePhoto?.let { (dateIso, path) ->
        AlertDialog(
            onDismissRequest = { pendingDeletePhoto = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = atmo.sheetBg,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Hapus Foto Ini?") },
            text = { Text("Foto akan dihapus dari rekap tanggal ${DateTimeHelper.formatShortDate(dateIso)}.") },
            confirmButton = {
                BouncyButton(
                    onClick = {
                        haptics.tapWarning()
                        onRemovePhoto(dateIso, path)
                        if (lightboxItem?.path == path) {
                            lightboxItem = null
                        }
                        pendingDeletePhoto = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = atmo.accentCoral),
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                BouncyTextButton(
                    onClick = {
                        haptics.tapSoft()
                        pendingDeletePhoto = null
                    },
                ) {
                    Text("Batal", color = Color(0xFFCBD5E1))
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("rekap_gallery_view"),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Gallery Summary & Filter Header
        item(key = "gallery_summary_header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = atmo.cardBg,
                    border = BorderStroke(1.dp, atmo.cardBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Galeri Jurnal Harian",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = if (totalPhotosCount == 0) {
                                    "Belum ada foto tersimpan di galeri"
                                } else {
                                    "$totalPhotosCount foto • ${allPhotoDays.size} hari terekam"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                            )
                        }
                        BouncyButton(
                            onClick = {
                                haptics.tapSoft()
                                onOpenCameraForDate(selectedDateIso)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = atmo.accentMint,
                                contentColor = atmo.onAccentMint,
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("gallery_capture_today_button"),
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Jepret", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val filters = buildList {
                        add(RekapGalleryFilter.ALL)
                        add(RekapGalleryFilter.TODAY)
                        if (selectedDateIso != todayIso) {
                            add(RekapGalleryFilter.SELECTED)
                        }
                    }
                    filters.forEach { filterOption ->
                        val tag = when (filterOption) {
                            RekapGalleryFilter.ALL -> "gallery_filter_all"
                            RekapGalleryFilter.TODAY -> "gallery_filter_today"
                            RekapGalleryFilter.SELECTED -> "gallery_filter_selected"
                        }
                        val label = if (filterOption == RekapGalleryFilter.SELECTED) {
                            DateTimeHelper.formatShortDate(selectedDateIso)
                        } else {
                            filterOption.label
                        }
                        BouncyFilterChip(
                            selected = activeFilter == filterOption,
                            onClick = {
                                haptics.tapTick()
                                activeFilter = filterOption
                            },
                            shape = RoundedCornerShape(14.dp),
                            label = { Text(label) },
                            modifier = Modifier.testTag(tag),
                        )
                    }
                }
            }
        }

        if (displayedDays.isEmpty()) {
            item(key = "gallery_empty_state") {
                val targetDate = when (activeFilter) {
                    RekapGalleryFilter.TODAY -> todayIso
                    RekapGalleryFilter.SELECTED -> selectedDateIso
                    RekapGalleryFilter.ALL -> selectedDateIso
                }
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = atmo.cardBg,
                    border = BorderStroke(1.dp, atmo.cardBorder),
                    modifier = Modifier.testTag("gallery_empty_state"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(atmo.accentMint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = atmo.accentMint,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        Text(
                            text = if (activeFilter == RekapGalleryFilter.ALL) {
                                "Galeri Rekap Masih Kosong"
                            } else {
                                "Belum Ada Foto di ${DateTimeHelper.formatShortDate(targetDate)}"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Ambil foto harian pakai kamera atau unggah momen terbaikmu supaya tersusun rapi di grid ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BouncyOutlinedButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onPickGalleryForDate(targetDate)
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, atmo.cardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Upload")
                            }
                            BouncyButton(
                                onClick = {
                                    haptics.tapSoft()
                                    onOpenCameraForDate(targetDate)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = atmo.accentMint,
                                    contentColor = atmo.onAccentMint,
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buka Kamera")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = displayedDays,
                key = { it.dateIso },
            ) { recap ->
                DailyGalleryGridCard(
                    recap = recap,
                    isToday = recap.dateIso == todayIso,
                    onTapPhoto = { path ->
                        lightboxItem = GalleryLightboxSelection(
                            dateIso = recap.dateIso,
                            path = path,
                            title = recap.title,
                            caption = recap.caption,
                            locationName = recap.locationName,
                        )
                    },
                    onAddMoreForDate = { onOpenCameraForDate(recap.dateIso) },
                    onExportCollage = { onExportCollage(recap.dateIso) },
                    onOpenCalendar = { onOpenCalendarForDate(recap.dateIso) },
                )
            }
        }
    }
}

@Composable
private fun DailyGalleryGridCard(
    recap: PhotoRecap,
    isToday: Boolean,
    onTapPhoto: (String) -> Unit,
    onAddMoreForDate: () -> Unit,
    onExportCollage: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val photos = recap.imagePaths.take(4)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = atmo.cardBg,
        border = BorderStroke(1.dp, atmo.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .testTag("gallery_day_card_${recap.dateIso}"),
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (isToday) {
                                "Hari Ini • ${DateTimeHelper.formatShortDate(recap.dateIso)}"
                            } else {
                                DateTimeHelper.formatIndonesianFullDate(recap.dateIso)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            color = atmo.accentMint.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "${photos.size}/4",
                                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = atmo.accentMint,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (recap.title.isNotBlank()) {
                        Text(
                            text = recap.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else if (recap.caption.isNotBlank()) {
                        Text(
                            text = recap.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (recap.locationName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.testTag("gallery_card_location_${recap.dateIso}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Lokasi rekap",
                                tint = atmo.accentMint,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = recap.locationName,
                                style = MaterialTheme.typography.labelSmall,
                                color = atmo.accentMint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BouncyIconButton(
                        onClick = {
                            haptics.tapSoft()
                            onOpenCalendar()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Buka catatan kalender tanggal ini",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    BouncyIconButton(
                        onClick = {
                            haptics.tapConfirm()
                            onExportCollage()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Ekspor kolase tanggal ini",
                            tint = atmo.accentMint,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            // Neat 3-column uniform photo grid (plus optional "+" add tile when < 4 photos)
            val gridSlots: List<Pair<Int, String?>> = buildList {
                photos.forEachIndexed { idx, path -> add(idx to path) }
                if (photos.size < 4) {
                    add(photos.size to null)
                }
            }
            val rows = gridSlots.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { (idx, path) ->
                            if (path != null) {
                                val tileInteraction = remember(path) { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .bouncyPress(tileInteraction, pressedScale = 0.94f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, atmo.cardBorder, RoundedCornerShape(16.dp))
                                        .clickable(
                                            interactionSource = tileInteraction,
                                            indication = null,
                                        ) {
                                            haptics.tapSoft()
                                            onTapPhoto(path)
                                        }
                                        .testTag("gallery_photo_tile_${recap.dateIso}_$idx"),
                                ) {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "Foto rekap ${idx + 1} tanggal ${recap.dateIso}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color(0x99000000)),
                                                ),
                                            ),
                                    )
                                    Surface(
                                        color = Color(0xB8080D21),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp),
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                            } else {
                                val addInteraction = remember(recap.dateIso) { MutableInteractionSource() }
                                Surface(
                                    onClick = {
                                        haptics.tapSoft()
                                        onAddMoreForDate()
                                    },
                                    interactionSource = addInteraction,
                                    shape = RoundedCornerShape(16.dp),
                                    color = atmo.cardAltBg.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, atmo.accentMint.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .bouncyPress(addInteraction, pressedScale = 0.94f)
                                        .testTag("gallery_add_tile_${recap.dateIso}"),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AddAPhoto,
                                            contentDescription = "Tambah foto di tanggal ini",
                                            tint = atmo.accentMint,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Tambah",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFFE2E8F0),
                                        )
                                    }
                                }
                            }
                        }
                        repeat((3 - rowItems.size).coerceAtLeast(0)) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
