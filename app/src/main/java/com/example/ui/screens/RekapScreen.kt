package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.DateTimeHelper
import com.example.domain.PhotoRecap
import com.example.domain.PhotoTextAlignment
import com.example.domain.PhotoTextStyle
import com.example.ui.JadwalNihUiState
import com.example.ui.components.BouncyButton
import com.example.ui.components.BouncyOutlinedButton
import com.example.ui.components.BouncyTextButton
import com.example.ui.components.CameraXViewfinder
import com.example.ui.components.DEFAULT_INDONESIA_LAT
import com.example.ui.components.DEFAULT_INDONESIA_LNG
import com.example.ui.components.MapLocationPickerSheet
import com.example.ui.components.OsmInteractiveMapView
import com.example.ui.components.OsmMarkerPoint
import com.example.ui.components.PostCapturePreviewSheet
import com.example.ui.components.RecapCalendarSheet
import com.example.ui.components.RekapGalleryView
import com.example.ui.components.bouncyPress
import com.example.ui.components.openInGoogleMaps
import com.example.ui.components.rememberBouncyScale
import com.example.ui.components.rememberCameraCaptureController
import com.example.ui.components.tapConfirm
import com.example.ui.components.tapSoft
import com.example.ui.components.tapTick
import com.example.ui.theme.LocalAdaptiveSpec
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.MonoSmallStyle
import java.io.File

private enum class RekapDisplayMode {
    CAMERA,
    GALLERY,
    MAP,
}

@Composable
fun RekapScreen(
    state: JadwalNihUiState,
    onSelectDate: (String) -> Unit,
    onAddPhotos: (String, List<Uri>) -> Unit,
    onSaveStyledPhoto: (
        dateIso: String,
        uri: Uri,
        textStyle: PhotoTextStyle,
        textAlignment: PhotoTextAlignment,
        overlayTimeLabel: String,
        title: String,
        caption: String,
    ) -> Unit,
    onRemovePhoto: (String, String) -> Unit,
    onSaveMeta: (String, String, String) -> Unit,
    onDeleteRecap: (String) -> Unit,
    onExportCollage: (String) -> Unit,
    onNotifyMessage: (String) -> Unit,
    onOverlayVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateRecapLocation: (dateIso: String, locationName: String, lat: Double, lng: Double) -> Unit = { _, _, _, _ -> },
) {
    val atmo = LocalAtmospherePalette.current
    val spec = LocalAdaptiveSpec.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val selectedDate = state.selectedRecapDateIso
    val todayIso = remember { DateTimeHelper.todayIso() }
    val currentRecap = state.photoRecaps[selectedDate]
    val photos = currentRecap?.imagePaths.orEmpty()

    var displayMode by rememberSaveable { mutableStateOf(RekapDisplayMode.CAMERA) }
    val cameraController = rememberCameraCaptureController()
    var useFrontCamera by remember { mutableStateOf(false) }
    var flashTrigger by remember { mutableIntStateOf(0) }
    var showUploadMenu by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCaptureDateIso by remember { mutableStateOf(selectedDate) }
    var pendingCaptureTimeLabel by remember { mutableStateOf("") }
    var pickerTargetDateIso by remember { mutableStateOf(selectedDate) }

    BackHandler(enabled = displayMode != RekapDisplayMode.CAMERA && !showCalendarSheet && pendingCaptureUri == null) {
        displayMode = RekapDisplayMode.CAMERA
    }

    LaunchedEffect(showCalendarSheet, pendingCaptureUri) {
        onOverlayVisibilityChange(showCalendarSheet || pendingCaptureUri != null)
    }

    val liveTimeLabel = remember(selectedDate) {
        DateTimeHelper.formatMinuteOfDay(DateTimeHelper.nowMinuteOfDay())
    }

    val handlePickedUris: (List<Uri>) -> Unit = { uris ->
        val targetDate = pickerTargetDateIso
        val targetExistingCount = state.photoRecaps[targetDate]?.imagePaths?.size ?: 0
        val available = (4 - targetExistingCount).coerceAtLeast(0)
        if (available == 0) {
            onNotifyMessage("Kuota 4/4 foto untuk tanggal ini sudah penuh.")
            showCalendarSheet = true
        } else if (uris.size == 1) {
            pendingCaptureDateIso = targetDate
            pendingCaptureTimeLabel = DateTimeHelper.formatMinuteOfDay(DateTimeHelper.nowMinuteOfDay())
            pendingCaptureUri = uris.first()
        } else if (uris.isNotEmpty()) {
            onAddPhotos(targetDate, uris.take(available))
        }
    }

    val multiPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
    ) { uris ->
        if (uris.isNotEmpty()) handlePickedUris(uris)
    }

    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) handlePickedUris(listOf(uri))
    }

    val documentPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) handlePickedUris(uris)
    }

    val launchDocumentPickerFor: (String) -> Unit = { dateIso ->
        pickerTargetDateIso = dateIso
        documentPhotoLauncher.launch(arrayOf("image/*"))
    }

    val launchGalleryPickerFor: (String) -> Unit = { dateIso ->
        pickerTargetDateIso = dateIso
        val slots = (4 - (state.photoRecaps[dateIso]?.imagePaths?.size ?: 0)).coerceAtLeast(1)
        try {
            if (slots == 1) {
                singlePhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            } else {
                multiPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }
        } catch (_: ActivityNotFoundException) {
            launchDocumentPickerFor(dateIso)
        }
    }

    val cleanupTempCapture: (Uri?) -> Unit = { uri ->
        if (uri?.scheme == "file") {
            uri.path?.let { p ->
                val f = File(p)
                if (f.name.startsWith("rekap_snap_")) {
                    f.delete()
                }
            }
        }
    }

    pendingCaptureUri?.let { capturedUri ->
        val existingForTarget = state.photoRecaps[pendingCaptureDateIso]
        PostCapturePreviewSheet(
            imageUri = capturedUri,
            targetDateIso = pendingCaptureDateIso,
            existingTitle = existingForTarget?.title ?: "",
            existingCaption = existingForTarget?.caption ?: "",
            timeLabel = pendingCaptureTimeLabel,
            onDismiss = {
                cleanupTempCapture(capturedUri)
                pendingCaptureUri = null
            },
            onConfirmSave = { textStyle, textAlignment, title, caption ->
                onSaveStyledPhoto(
                    pendingCaptureDateIso,
                    capturedUri,
                    textStyle,
                    textAlignment,
                    pendingCaptureTimeLabel,
                    title,
                    caption,
                )
                pendingCaptureUri = null
            },
        )
    }

    if (showCalendarSheet) {
        RecapCalendarSheet(
            selectedDateIso = selectedDate,
            photoRecaps = state.photoRecaps,
            allTasks = state.allTasksWithStatus,
            isExportingCollage = state.isExportingCollage,
            onSelectDate = onSelectDate,
            onJumpToCameraForDate = { targetDate ->
                onSelectDate(targetDate)
                displayMode = RekapDisplayMode.CAMERA
                showCalendarSheet = false
            },
            onPickGalleryForDate = { targetDate ->
                launchGalleryPickerFor(targetDate)
            },
            onPickFileForDate = { targetDate ->
                launchDocumentPickerFor(targetDate)
            },
            onRemovePhoto = onRemovePhoto,
            onSaveMeta = onSaveMeta,
            onDeleteRecap = onDeleteRecap,
            onExportCollage = onExportCollage,
            onDismiss = { showCalendarSheet = false },
            defaultLocationLat = state.currentLatitude,
            defaultLocationLng = state.currentLongitude,
            onUpdateRecapLocation = onUpdateRecapLocation,
        )
    }

    val recordedDaysCount = remember(state.photoRecaps) {
        state.photoRecaps.values.count { it.imagePaths.isNotEmpty() || it.title.isNotBlank() || it.caption.isNotBlank() }
    }
    val totalSavedPhotos = remember(state.photoRecaps) {
        state.photoRecaps.values.sumOf { recap ->
            recap.imagePaths.count { it.isNotBlank() && File(it).exists() }
        }
    }

    val flipRotation by animateFloatAsState(
        targetValue = if (useFrontCamera) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "camera_flip_rotation",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Segmented Switch: Kamera <-> Galeri <-> Peta
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = atmo.cardBg,
            border = BorderStroke(1.dp, atmo.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RekapModeSegment(
                    selected = displayMode == RekapDisplayMode.CAMERA,
                    label = "Kamera",
                    icon = Icons.Filled.CameraAlt,
                    badgeCount = null,
                    accentColor = atmo.accentMint,
                    onAccentColor = atmo.onAccentMint,
                    onClick = {
                        haptics.tapTick()
                        displayMode = RekapDisplayMode.CAMERA
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rekap_mode_camera"),
                )
                RekapModeSegment(
                    selected = displayMode == RekapDisplayMode.GALLERY,
                    label = "Galeri",
                    icon = Icons.Filled.PhotoLibrary,
                    badgeCount = totalSavedPhotos.takeIf { it > 0 },
                    accentColor = atmo.accentMint,
                    onAccentColor = atmo.onAccentMint,
                    onClick = {
                        haptics.tapTick()
                        displayMode = RekapDisplayMode.GALLERY
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rekap_mode_gallery"),
                )
                RekapModeSegment(
                    selected = displayMode == RekapDisplayMode.MAP,
                    label = "Peta",
                    icon = Icons.Filled.Map,
                    badgeCount = null,
                    accentColor = atmo.accentCobalt,
                    onAccentColor = atmo.onAccentCobalt,
                    onClick = {
                        haptics.tapTick()
                        displayMode = RekapDisplayMode.MAP
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rekap_mode_map"),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        AnimatedContent(
            targetState = displayMode,
            transitionSpec = {
                (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(tween(150)))
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "rekap_display_mode_switch",
        ) { mode ->
            when (mode) {
                RekapDisplayMode.CAMERA -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Target Date & Daily Quota Row
                        val datePillInteraction = remember { MutableInteractionSource() }
                        val quotaPillInteraction = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                onClick = {
                                    haptics.tapSoft()
                                    showCalendarSheet = true
                                },
                                interactionSource = datePillInteraction,
                                color = atmo.cardBg,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, atmo.cardBorder),
                                modifier = Modifier
                                    .bouncyPress(datePillInteraction, pressedScale = 0.92f)
                                    .minimumInteractiveComponentSize(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = "Pilih tanggal rekap",
                                        tint = atmo.accentMint,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (selectedDate == todayIso) {
                                            "Hari Ini • ${DateTimeHelper.formatShortDate(selectedDate)}"
                                        } else {
                                            DateTimeHelper.formatShortDate(selectedDate)
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selectedDate != todayIso) {
                                    BouncyTextButton(
                                        onClick = {
                                            haptics.tapSoft()
                                            onSelectDate(todayIso)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Text("Hari Ini", color = atmo.accentMint)
                                    }
                                }
                                Surface(
                                    onClick = {
                                        haptics.tapSoft()
                                        displayMode = RekapDisplayMode.GALLERY
                                    },
                                    interactionSource = quotaPillInteraction,
                                    color = atmo.cardBg,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, atmo.cardBorder),
                                    modifier = Modifier
                                        .bouncyPress(quotaPillInteraction, pressedScale = 0.92f)
                                        .minimumInteractiveComponentSize(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        val thumb = photos.lastOrNull()?.takeIf { File(it).exists() }
                                        if (thumb != null) {
                                            AsyncImage(
                                                model = File(thumb),
                                                contentDescription = "Foto terbaru hari ini",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape),
                                            )
                                        }
                                        AnimatedContent(
                                            targetState = "${photos.size}/4",
                                            transitionSpec = {
                                                (fadeIn(tween(180)) + scaleIn(initialScale = 0.85f))
                                                    .togetherWith(fadeOut(tween(120)))
                                            },
                                            label = "rekap_quota_badge",
                                        ) { quotaText ->
                                            Text(
                                                text = quotaText,
                                                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.currentLocationName.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = atmo.cardBg,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, atmo.cardBorder),
                                modifier = Modifier.testTag("rekap_camera_location_pill"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Lokasi Rekap",
                                        tint = atmo.accentMint,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "Lokasi Rekap: ${state.currentLocationName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Center Rounded Camera Viewfinder (with bottom-left morphing zoom control)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            CameraXViewfinder(
                                controller = cameraController,
                                useFrontCamera = useFrontCamera,
                                liveTimeLabel = liveTimeLabel,
                                flashTrigger = flashTrigger,
                                onRequestUpload = {
                                    if (photos.size >= 4) {
                                        onNotifyMessage("Kuota 4/4 foto untuk tanggal ini sudah penuh.")
                                        showCalendarSheet = true
                                    } else {
                                        showUploadMenu = true
                                    }
                                },
                                onPermissionResult = { granted ->
                                    if (!granted) {
                                        onNotifyMessage("Izin kamera ditolak. Kamu masih bisa upload foto lewat tombol di kiri.")
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )

                            if (photos.size >= 4) {
                                val fullPillInteraction = remember { MutableInteractionSource() }
                                Surface(
                                    onClick = {
                                        haptics.tapSoft()
                                        showCalendarSheet = true
                                    },
                                    interactionSource = fullPillInteraction,
                                    color = atmo.cardBg,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, atmo.accentCoral.copy(alpha = 0.75f)),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                        .bouncyPress(fullPillInteraction, pressedScale = 0.92f),
                                ) {
                                    Text(
                                        text = "Kuota 4/4 penuh • Ketuk kalender",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Camera Control Deck: [Upload]  (( Shutter ))  [Flip]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 360.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Left: Upload button (Gallery or File)
                            Box(contentAlignment = Alignment.Center) {
                                val uploadInteraction = remember { MutableInteractionSource() }
                                val uploadScale = rememberBouncyScale(uploadInteraction, pressedScale = 0.90f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Surface(
                                        onClick = {
                                            haptics.tapSoft()
                                            if (photos.size >= 4) {
                                                onNotifyMessage("Kuota 4/4 foto hari ini sudah penuh. Hapus salah satu di Kalender dulu.")
                                                showCalendarSheet = true
                                            } else {
                                                showUploadMenu = true
                                            }
                                        },
                                        interactionSource = uploadInteraction,
                                        shape = CircleShape,
                                        color = atmo.cardBg,
                                        border = BorderStroke(1.dp, atmo.cardBorder),
                                        modifier = Modifier
                                            .size(56.dp)
                                            .graphicsLayer {
                                                scaleX = uploadScale
                                                scaleY = uploadScale
                                            }
                                            .minimumInteractiveComponentSize()
                                            .testTag("rekap_upload_button"),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoLibrary,
                                                contentDescription = "Upload foto dari galeri atau file",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Upload",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                DropdownMenu(
                                    expanded = showUploadMenu,
                                    onDismissRequest = { showUploadMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Pilih dari Galeri") },
                                        leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null) },
                                        onClick = {
                                            haptics.tapSoft()
                                            showUploadMenu = false
                                            launchGalleryPickerFor(selectedDate)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pilih File (PNG, JPG, WebP)") },
                                        leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                                        onClick = {
                                            haptics.tapSoft()
                                            showUploadMenu = false
                                            launchDocumentPickerFor(selectedDate)
                                        },
                                    )
                                }
                            }

                            // Center: Iconic Glowing Shutter Button
                            val shutterInteraction = remember { MutableInteractionSource() }
                            val shutterScale = rememberBouncyScale(shutterInteraction, pressedScale = 0.86f)
                            Box(
                                modifier = Modifier
                                    .size(82.dp)
                                    .graphicsLayer {
                                        scaleX = shutterScale
                                        scaleY = shutterScale
                                    }
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = CircleShape,
                                        ambientColor = atmo.accentMint.copy(alpha = 0.35f),
                                        spotColor = atmo.accentMint.copy(alpha = 0.40f),
                                    )
                                    .clip(CircleShape)
                                    .border(
                                        width = 4.dp,
                                        color = atmo.accentMint,
                                        shape = CircleShape,
                                    )
                                    .clickable(
                                        interactionSource = shutterInteraction,
                                        indication = null,
                                        onClick = {
                                            if (photos.size >= 4) {
                                                haptics.tapSoft()
                                                onNotifyMessage("Kuota 4/4 foto hari ini sudah penuh. Hapus dulu di Kalender ya.")
                                                showCalendarSheet = true
                                                return@clickable
                                            }
                                            haptics.tapConfirm()
                                            flashTrigger += 1
                                            cameraController.capturePhoto(
                                                onCaptured = { uri ->
                                                    pendingCaptureDateIso = selectedDate
                                                    pendingCaptureTimeLabel = DateTimeHelper.formatMinuteOfDay(DateTimeHelper.nowMinuteOfDay())
                                                    pendingCaptureUri = uri
                                                },
                                                onError = { err ->
                                                    onNotifyMessage(err)
                                                },
                                            )
                                        },
                                    )
                                    .padding(9.dp)
                                    .testTag("camera_shutter_button"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            if (cameraController.isCameraReady) Color.White else Color(0xFFCBD5F5),
                                        ),
                                )
                            }

                            // Right: Flip Camera Button
                            val flipInteraction = remember { MutableInteractionSource() }
                            val flipScale = rememberBouncyScale(flipInteraction, pressedScale = 0.90f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Surface(
                                    onClick = {
                                        haptics.tapSoft()
                                        useFrontCamera = !useFrontCamera
                                    },
                                    interactionSource = flipInteraction,
                                    shape = CircleShape,
                                    color = atmo.cardBg,
                                    border = BorderStroke(1.dp, atmo.cardBorder),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .graphicsLayer {
                                            scaleX = flipScale
                                            scaleY = flipScale
                                        }
                                        .minimumInteractiveComponentSize()
                                        .testTag("camera_flip_button"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Cameraswitch,
                                            contentDescription = "Pindah kamera depan atau belakang",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .graphicsLayer { rotationY = flipRotation },
                                        )
                                    }
                                }
                                Text(
                                    text = if (useFrontCamera) "Depan" else "Belakang",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Bottom Upward Arrow (^) Trigger to open Calendar Sheet
                        val calInteraction = remember { MutableInteractionSource() }
                        val calScale = rememberBouncyScale(calInteraction, pressedScale = 0.94f)
                        Surface(
                            onClick = {
                                haptics.tapSoft()
                                showCalendarSheet = true
                            },
                            interactionSource = calInteraction,
                            color = atmo.cardBg,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, atmo.accentMint.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = calScale
                                    scaleY = calScale
                                }
                                .minimumInteractiveComponentSize()
                                .testTag("open_recap_calendar_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Buka kalender rekap",
                                    tint = atmo.accentMint,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = if (recordedDaysCount > 0) {
                                        "Kalender Rekap • $recordedDaysCount hari"
                                    } else {
                                        "Kalender Rekap"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                            }
                        }

                        Spacer(Modifier.height(spec.bottomNavClearance))
                    }
                }

                RekapDisplayMode.GALLERY -> {
                    RekapGalleryView(
                        photoRecaps = state.photoRecaps,
                        selectedDateIso = selectedDate,
                        todayIso = todayIso,
                        isExportingCollage = state.isExportingCollage,
                        onOpenCameraForDate = { targetDate ->
                            onSelectDate(targetDate)
                            displayMode = RekapDisplayMode.CAMERA
                        },
                        onPickGalleryForDate = { targetDate ->
                            launchGalleryPickerFor(targetDate)
                        },
                        onRemovePhoto = onRemovePhoto,
                        onExportCollage = onExportCollage,
                        onOpenCalendarForDate = { targetDate ->
                            onSelectDate(targetDate)
                            showCalendarSheet = true
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                RekapDisplayMode.MAP -> {
                    RekapMapView(
                        photoRecaps = state.photoRecaps,
                        selectedDateIso = selectedDate,
                        defaultLocationName = state.currentLocationName,
                        defaultLocationLat = state.currentLatitude,
                        defaultLocationLng = state.currentLongitude,
                        bottomClearance = spec.bottomNavClearance,
                        onSelectDate = onSelectDate,
                        onOpenCalendarDetail = { targetDate ->
                            onSelectDate(targetDate)
                            showCalendarSheet = true
                        },
                        onUpdateRecapLocation = onUpdateRecapLocation,
                        onJumpToCamera = {
                            displayMode = RekapDisplayMode.CAMERA
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RekapModeSegment(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeCount: Int?,
    accentColor: Color,
    onAccentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) accentColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rekap_seg_bg_$label",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) onAccentColor else Color(0xFFCBD5E1),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rekap_seg_fg_$label",
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        modifier = modifier
            .bouncyPress(interaction, pressedScale = 0.94f)
            .minimumInteractiveComponentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (badgeCount != null) {
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) onAccentColor.copy(alpha = 0.16f) else accentColor.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) onAccentColor else accentColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RekapMapView(
    photoRecaps: Map<String, PhotoRecap>,
    selectedDateIso: String,
    defaultLocationName: String,
    defaultLocationLat: Double?,
    defaultLocationLng: Double?,
    bottomClearance: androidx.compose.ui.unit.Dp,
    onSelectDate: (String) -> Unit,
    onOpenCalendarDetail: (String) -> Unit,
    onUpdateRecapLocation: (dateIso: String, locationName: String, lat: Double, lng: Double) -> Unit,
    onJumpToCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atmo = LocalAtmospherePalette.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val recapsWithPhotos = remember(photoRecaps) {
        photoRecaps.values
            .filter { it.imagePaths.isNotEmpty() || it.locationLat != null || it.locationName.isNotBlank() }
            .sortedByDescending { it.dateIso }
    }

    var focusedDateIso by remember(selectedDateIso, recapsWithPhotos) {
        mutableStateOf(
            recapsWithPhotos.firstOrNull { it.dateIso == selectedDateIso }?.dateIso
                ?: recapsWithPhotos.firstOrNull()?.dateIso
                ?: selectedDateIso,
        )
    }
    var showMovePinSheet by remember { mutableStateOf(false) }

    val activeRecap = photoRecaps[focusedDateIso] ?: recapsWithPhotos.firstOrNull()
    val fallbackLat = defaultLocationLat ?: DEFAULT_INDONESIA_LAT
    val fallbackLng = defaultLocationLng ?: DEFAULT_INDONESIA_LNG

    val resolvedLat = activeRecap?.locationLat ?: fallbackLat
    val resolvedLng = activeRecap?.locationLng ?: fallbackLng
    val isFallbackLocation = activeRecap?.locationLat == null || activeRecap.locationLng == null

    if (showMovePinSheet && activeRecap != null) {
        MapLocationPickerSheet(
            initialLocationName = activeRecap.locationName.ifBlank { defaultLocationName },
            initialLatitude = resolvedLat,
            initialLongitude = resolvedLng,
            onConfirmLocation = { name, lat, lng ->
                onUpdateRecapLocation(activeRecap.dateIso, name, lat, lng)
            },
            onDismiss = { showMovePinSheet = false },
        )
    }

    if (recapsWithPhotos.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = bottomClearance)
                .testTag("rekap_map_view"),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = atmo.cardBg,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, atmo.cardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        tint = atmo.accentMint,
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = "Belum Ada Foto Rekap di Peta",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = "Ambil foto atau unggah dari galeri, lalu lihat titik lokasinya di peta dan Google Maps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                    )
                    BouncyButton(
                        onClick = {
                            haptics.tapSoft()
                            onJumpToCamera()
                        },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buka Kamera Rekap")
                    }
                }
            }
        }
        return
    }

    val markers = remember(recapsWithPhotos, focusedDateIso, fallbackLat, fallbackLng) {
        recapsWithPhotos.mapIndexed { idx, recap ->
            val lat = recap.locationLat ?: (fallbackLat + idx * 0.0004)
            val lng = recap.locationLng ?: (fallbackLng + idx * 0.0004)
            OsmMarkerPoint(
                id = recap.dateIso,
                title = recap.title.ifBlank { "Rekap ${DateTimeHelper.formatShortDate(recap.dateIso)}" },
                subtitle = recap.locationName.ifBlank { defaultLocationName.ifBlank { "Lokasi Pengaturan" } },
                latitude = lat,
                longitude = lng,
                isHighlighted = recap.dateIso == focusedDateIso,
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomClearance)
            .testTag("rekap_map_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, atmo.cardBorder, RoundedCornerShape(22.dp)),
        ) {
            OsmInteractiveMapView(
                centerLat = resolvedLat,
                centerLng = resolvedLng,
                zoomLevel = 13.5,
                markers = markers,
                onMarkerClick = { point ->
                    haptics.tapSoft()
                    focusedDateIso = point.id
                    onSelectDate(point.id)
                },
                modifier = Modifier.fillMaxSize(),
            )

            Surface(
                color = Color(0xCC0E121B),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            ) {
                Text(
                    text = "${recapsWithPhotos.size} momen di peta",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        if (activeRecap != null) {
            val thumbPath = activeRecap.imagePaths.firstOrNull { File(it).exists() }
            Surface(
                color = atmo.cardBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, atmo.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rekap_map_selected_card"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (thumbPath != null) {
                            AsyncImage(
                                model = File(thumbPath),
                                contentDescription = "Foto rekap ${activeRecap.dateIso}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                        } else {
                            Surface(
                                color = atmo.accentMint.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = atmo.accentMint)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = activeRecap.title.ifBlank { DateTimeHelper.formatIndonesianFullDate(activeRecap.dateIso) },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append(activeRecap.locationName.ifBlank { defaultLocationName.ifBlank { "Jakarta, Indonesia" } })
                                    if (isFallbackLocation) append(" (Perkiraan Pengaturan)")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = atmo.accentMint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${DateTimeHelper.formatShortDate(activeRecap.dateIso)} • ${activeRecap.imagePaths.size} foto",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1),
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
                                showMovePinSheet = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("rekap_map_move_pin_button"),
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Geser Pin", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        BouncyOutlinedButton(
                            onClick = {
                                haptics.tapSoft()
                                onOpenCalendarDetail(activeRecap.dateIso)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("rekap_map_detail_button"),
                        ) {
                            Text("Detail", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        BouncyButton(
                            onClick = {
                                haptics.tapConfirm()
                                openInGoogleMaps(
                                    context = context,
                                    latitude = resolvedLat,
                                    longitude = resolvedLng,
                                    label = activeRecap.locationName.ifBlank { "Rekap ${activeRecap.dateIso}" },
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .minimumInteractiveComponentSize()
                                .testTag("rekap_map_google_maps_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Google Maps", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

