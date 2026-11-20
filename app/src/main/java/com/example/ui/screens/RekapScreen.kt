package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoRecap
import com.example.ui.JadwalNihUiState
import com.example.ui.components.FrostedGlassCard
import com.example.ui.theme.LocalAtmospherePalette
import java.io.File

@Composable
fun RekapScreen(
    state: JadwalNihUiState,
    onSelectDate: (String) -> Unit,
    onAddPhotos: (String, List<Uri>) -> Unit,
    onRemovePhoto: (String, String) -> Unit,
    onSaveMeta: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val selectedDate = state.selectedRecapDateIso
    val currentRecap = state.photoRecaps[selectedDate]
    val photos = currentRecap?.imagePaths.orEmpty()

    var titleInput by remember(selectedDate, currentRecap?.title) {
        mutableStateOf(currentRecap?.title ?: "")
    }
    var captionInput by remember(selectedDate, currentRecap?.caption) {
        mutableStateOf(currentRecap?.caption ?: "")
    }
    var zoomedImagePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedDate, currentRecap) {
        titleInput = currentRecap?.title ?: ""
        captionInput = currentRecap?.caption ?: ""
    }

    val remainingSlots = (4 - photos.size).coerceAtLeast(1)
    val multiPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddPhotos(selectedDate, uris.take(remainingSlots))
        }
    }

    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onAddPhotos(selectedDate, listOf(uri))
        }
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
                    IconButton(
                        onClick = { zoomedImagePath = null },
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

    val pastRecaps = remember(state.photoRecaps, selectedDate) {
        state.photoRecaps.values
            .filter { it.imagePaths.isNotEmpty() || it.title.isNotBlank() || it.caption.isNotBlank() }
            .sortedByDescending { it.dateIso }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "recap_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Rekap Harian",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Simpan 1–4 foto momen & catatan singkat tiap hari.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "recap_date_bar") {
            FrostedGlassCard(cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { onSelectDate(DateTimeHelper.addDays(selectedDate, -1)) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Hari sebelumnya")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DateTimeHelper.formatIndonesianFullDate(selectedDate),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (selectedDate != DateTimeHelper.todayIso()) {
                            TextButton(onClick = { onSelectDate(DateTimeHelper.todayIso()) }) {
                                Text("Hari Ini", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    IconButton(
                        onClick = { onSelectDate(DateTimeHelper.addDays(selectedDate, 1)) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Hari berikutnya")
                    }
                }
            }
        }

        item(key = "recap_collage_card") {
            FrostedGlassCard(cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Foto (${photos.size}/4)",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (photos.size < 4) {
                            OutlinedButton(
                                onClick = {
                                    if (remainingSlots == 1) {
                                        singlePhotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    } else {
                                        multiPhotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("add_recap_photo_button"),
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Tambah Foto")
                            }
                        }
                    }

                    if (photos.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    multiPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = "Unggah foto rekap",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Ketuk buat pilih 1–4 foto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = "Layout kolase otomatis menyesuaikan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        AutoPhotoCollage(
                            paths = photos,
                            onZoom = { zoomedImagePath = it },
                            onRemove = { onRemovePhoto(selectedDate, it) },
                        )
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Judul Singkat") },
                        placeholder = { Text("Misal: Hari Produktif di Kafe") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recap_title_input"),
                    )

                    OutlinedTextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        label = { Text("Catatan Singkat") },
                        placeholder = { Text("Cerita singkat tentang hari ini...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recap_caption_input"),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(atmo.heroGradStart, atmo.heroGradEnd),
                                ),
                            ),
                    ) {
                        Button(
                            onClick = { onSaveMeta(selectedDate, titleInput, captionInput) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("save_recap_meta_button"),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan Rekap")
                        }
                    }
                }
            }
        }

        if (pastRecaps.isNotEmpty()) {
            item(key = "past_recaps_title") {
                Text(
                    text = "Arsip Rekap (${pastRecaps.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            items(items = pastRecaps, key = { it.dateIso }) { recap ->
                RecapArchiveRow(
                    recap = recap,
                    isSelected = recap.dateIso == selectedDate,
                    onClick = { onSelectDate(recap.dateIso) },
                )
            }
        }
    }
}

@Composable
private fun AutoPhotoCollage(
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onZoom),
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = "Foto rekap harian",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            onClick = onRemove,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
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

@Composable
private fun RecapArchiveRow(
    recap: PhotoRecap,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val atmo = LocalAtmospherePalette.current
    FrostedGlassCard(
        onClick = onClick,
        cornerRadius = 16.dp,
        borderBrush = if (isSelected) {
            Brush.linearGradient(listOf(atmo.heroGradStart, atmo.heroGradEnd))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val thumb = recap.imagePaths.firstOrNull()
            if (thumb != null && File(thumb).exists()) {
                AsyncImage(
                    model = File(thumb),
                    contentDescription = "Thumbnail rekap",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = DateTimeHelper.formatIndonesianFullDate(recap.dateIso),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = recap.title.ifBlank { "${recap.imagePaths.size} foto rekap" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (recap.caption.isNotBlank()) {
                    Text(
                        text = recap.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
