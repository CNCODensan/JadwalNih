package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.LocalAtmospherePalette
import com.example.ui.theme.MonoSmallStyle
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal const val MAX_REKAP_CAMERA_ZOOM = 3.7f

enum class CameraZoomUiMode {
    COLLAPSED,
    PRESETS,
    GEAR_DIAL,
}

@Stable
class CameraCaptureController(
    private val context: Context,
) {
    internal var imageCapture: ImageCapture? by mutableStateOf(null)
    internal var camera: Camera? by mutableStateOf(null)
    var isFrontCamera: Boolean by mutableStateOf(false)
    var isCameraReady: Boolean by mutableStateOf(false)
    var isCapturing: Boolean by mutableStateOf(false)
    var zoomRatio: Float by mutableFloatStateOf(1f)
    var minZoomRatio: Float by mutableFloatStateOf(1f)
    var maxZoomRatio: Float by mutableFloatStateOf(MAX_REKAP_CAMERA_ZOOM)

    fun setZoom(targetRatio: Float) {
        val safeMin = minZoomRatio.coerceAtMost(1f)
        val safeMax = minOf(maxZoomRatio, MAX_REKAP_CAMERA_ZOOM).coerceAtLeast(1f)
        val clamped = targetRatio.coerceIn(safeMin, safeMax)
        zoomRatio = clamped
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    fun applyZoomScaleFactor(factor: Float) {
        setZoom(zoomRatio * factor)
    }

    fun capturePhoto(
        onCaptured: (Uri) -> Unit,
        onError: (String) -> Unit,
    ) {
        val captureUseCase = imageCapture
        if (captureUseCase == null || !isCameraReady || isCapturing) {
            onError("Kamera belum siap. Coba sebentar lagi atau pakai tombol Upload.")
            return
        }
        isCapturing = true
        val outputFile = File(context.cacheDir, "rekap_snap_${System.currentTimeMillis()}.jpg")
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = isFrontCamera
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile)
            .setMetadata(metadata)
            .build()

        captureUseCase.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    onCaptured(outputFileResults.savedUri ?: Uri.fromFile(outputFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    outputFile.delete()
                    onError("Gagal menjepret foto: ${exception.localizedMessage ?: "Kamera sibuk"}")
                }
            },
        )
    }
}

@Composable
fun rememberCameraCaptureController(): CameraCaptureController {
    val context = LocalContext.current
    return remember(context) { CameraCaptureController(context) }
}

internal fun formatZoomLabel(ratio: Float): String {
    val roundedTenth = (ratio * 10f).roundToInt()
    return if (roundedTenth % 10 == 0) {
        "${roundedTenth / 10}x"
    } else {
        String.format(Locale.US, "%.1fx", roundedTenth / 10f)
    }
}

internal fun buildCameraZoomPresets(maxZoomRatio: Float): List<Float> {
    val cappedMax = minOf(maxZoomRatio, MAX_REKAP_CAMERA_ZOOM).coerceAtLeast(1f)
    return if (cappedMax >= 3.65f) {
        listOf(1f, 2f, MAX_REKAP_CAMERA_ZOOM)
    } else if (cappedMax >= 2.0f) {
        listOf(1f, 2f, cappedMax)
    } else {
        listOf(1f, ((1f + cappedMax) / 2f), cappedMax)
            .distinctBy { (it * 10f).roundToInt() }
    }
}

@Composable
fun CameraXViewfinder(
    controller: CameraCaptureController,
    useFrontCamera: Boolean,
    liveTimeLabel: String,
    flashTrigger: Int,
    onRequestUpload: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraUnavailableMessage by remember { mutableStateOf<String?>(null) }
    var zoomUiMode by remember { mutableStateOf(CameraZoomUiMode.COLLAPSED) }
    var zoomInteractionTick by remember { mutableIntStateOf(0) }
    var lastPinchDetent by remember { mutableIntStateOf(5) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        onPermissionResult(granted)
    }

    val flashAlpha by animateFloatAsState(
        targetValue = if (flashTrigger > 0 && controller.isCapturing) 0.72f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "shutter_flash",
    )

    val frameShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .clip(frameShape)
            .background(Color(0xFF0E121B))
            .border(
                width = 1.5.dp,
                color = atmo.accentMint.copy(alpha = 0.72f),
                shape = frameShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(atmo.accentMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = atmo.onAccentMint,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Aktifkan Kamera Rekap",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Biar bisa langsung jepret momen hari ini dari layar utama Rekap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                BouncyButton(
                    onClick = {
                        haptics.tapSoft()
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = atmo.accentMint,
                        contentColor = atmo.onAccentMint,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("grant_camera_permission_button"),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Izinkan Kamera")
                }
                Spacer(Modifier.height(8.dp))
                BouncyOutlinedButton(
                    onClick = {
                        haptics.tapSoft()
                        onRequestUpload()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Upload Foto Saja")
                }
            }
        } else {
            val previewView = remember(context) {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            DisposableEffect(lifecycleOwner, useFrontCamera, hasCameraPermission) {
                controller.isFrontCamera = useFrontCamera
                controller.isCameraReady = false
                controller.zoomRatio = 1f
                zoomUiMode = CameraZoomUiMode.COLLAPSED
                cameraUnavailableMessage = null

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val executor = ContextCompat.getMainExecutor(context)
                var boundProvider: ProcessCameraProvider? = null
                var disposed = false

                val listener = Runnable {
                    if (disposed) return@Runnable
                    runCatching {
                        val provider = cameraProviderFuture.get()
                        if (disposed) return@Runnable
                        boundProvider = provider
                        val desiredSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        val fallbackSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        val activeSelector = when {
                            provider.hasCamera(desiredSelector) -> desiredSelector
                            provider.hasCamera(fallbackSelector) -> fallbackSelector
                            else -> null
                        }
                        if (activeSelector == null) {
                            cameraUnavailableMessage = "Kamera perangkat tidak terdeteksi. Gunakan tombol Upload di kiri."
                            return@runCatching
                        }

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        provider.unbindAll()
                        val boundCamera = provider.bindToLifecycle(
                            lifecycleOwner,
                            activeSelector,
                            preview,
                            imageCapture,
                        )
                        controller.camera = boundCamera
                        controller.imageCapture = imageCapture
                        controller.isFrontCamera = (activeSelector == CameraSelector.DEFAULT_FRONT_CAMERA)

                        val zoomState = boundCamera.cameraInfo.zoomState.value
                        val hwMin = (zoomState?.minZoomRatio ?: 1f).coerceAtMost(1f)
                        val hwMax = minOf(zoomState?.maxZoomRatio ?: MAX_REKAP_CAMERA_ZOOM, MAX_REKAP_CAMERA_ZOOM)
                            .coerceAtLeast(1f)
                        controller.minZoomRatio = hwMin
                        controller.maxZoomRatio = hwMax
                        controller.setZoom(1f)
                        controller.isCameraReady = true
                    }.onFailure {
                        controller.isCameraReady = false
                        controller.camera = null
                        cameraUnavailableMessage = "Pratinjau kamera sedang tidak tersedia. Pakai Upload di kiri."
                    }
                }

                cameraProviderFuture.addListener(listener, executor)

                onDispose {
                    disposed = true
                    controller.isCameraReady = false
                    controller.imageCapture = null
                    controller.camera = null
                    controller.zoomRatio = 1f
                    runCatching { boundProvider?.unbindAll() }
                }
            }

            // Keep ZoomState limits synced if camera reports updated hardware bounds asynchronously
            val boundCamera = controller.camera
            DisposableEffect(boundCamera, lifecycleOwner) {
                val zoomLiveData = boundCamera?.cameraInfo?.zoomState
                val observer = Observer<androidx.camera.core.ZoomState> { z ->
                    val safeMin = z.minZoomRatio.coerceAtMost(1f)
                    val safeMax = minOf(z.maxZoomRatio, MAX_REKAP_CAMERA_ZOOM).coerceAtLeast(1f)
                    controller.minZoomRatio = safeMin
                    controller.maxZoomRatio = safeMax
                    controller.zoomRatio = z.zoomRatio.coerceIn(safeMin, safeMax)
                }
                zoomLiveData?.observe(lifecycleOwner, observer)
                onDispose {
                    zoomLiveData?.removeObserver(observer)
                }
            }

            if (cameraUnavailableMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = cameraUnavailableMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    BouncyOutlinedButton(
                        onClick = {
                            haptics.tapSoft()
                            onRequestUpload()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih dari Galeri / File")
                    }
                }
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(controller.isCameraReady) {
                            if (!controller.isCameraReady) return@pointerInput
                            detectTransformGestures { _, _, zoomChange, _ ->
                                if (abs(zoomChange - 1f) > 0.002f) {
                                    controller.applyZoomScaleFactor(zoomChange)
                                    zoomUiMode = CameraZoomUiMode.GEAR_DIAL
                                    zoomInteractionTick += 1
                                    val detent = (controller.zoomRatio * 5f).roundToInt()
                                    if (detent != lastPinchDetent) {
                                        lastPinchDetent = detent
                                        haptics.tapTick()
                                    }
                                }
                            }
                        },
                )
            }

            // Bottom-left compact zoom button -> 3 presets (1x, 2x, 3.7x) -> camera gear dial on pinch
            val safeMaxZoom = minOf(controller.maxZoomRatio, MAX_REKAP_CAMERA_ZOOM)
            if (controller.isCameraReady && safeMaxZoom > 1.08f) {
                CameraZoomControl(
                    zoomRatio = controller.zoomRatio,
                    minZoomRatio = controller.minZoomRatio,
                    maxZoomRatio = safeMaxZoom,
                    mode = zoomUiMode,
                    interactionTick = zoomInteractionTick,
                    onExpandPresets = {
                        haptics.tapSoft()
                        zoomUiMode = CameraZoomUiMode.PRESETS
                        zoomInteractionTick += 1
                    },
                    onSelectPreset = { targetZoom ->
                        haptics.tapTick()
                        controller.setZoom(targetZoom)
                        zoomInteractionTick += 1
                    },
                    onAutoCollapse = {
                        zoomUiMode = CameraZoomUiMode.COLLAPSED
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp),
                )
            }

            // Compact lens indicator pill (top-right inside viewfinder)
            if (controller.isCameraReady) {
                Surface(
                    color = Color(0x99080D21),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
                    Text(
                        text = if (controller.isFrontCamera) "Depan" else "Belakang",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            // Compact live time badge pill at bottom-right so bottom-left zoom bar has ample room
            Surface(
                color = Color(0xA8080D21),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFBAE6FD),
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = liveTimeLabel,
                        style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }

            if (flashAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha)),
                )
            }
        }
    }
}

@Composable
fun CameraZoomControl(
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    mode: CameraZoomUiMode,
    interactionTick: Int,
    onExpandPresets: () -> Unit,
    onSelectPreset: (Float) -> Unit,
    onAutoCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    autoCollapsePresetsMs: Long = 2200L,
    autoCollapsePinchMs: Long = 1600L,
) {
    val atmo = LocalAtmospherePalette.current
    val presets = remember(maxZoomRatio) { buildCameraZoomPresets(maxZoomRatio) }

    LaunchedEffect(mode, interactionTick, zoomRatio) {
        when (mode) {
            CameraZoomUiMode.PRESETS -> {
                delay(autoCollapsePresetsMs)
                onAutoCollapse()
            }
            CameraZoomUiMode.GEAR_DIAL -> {
                delay(autoCollapsePinchMs)
                onAutoCollapse()
            }
            CameraZoomUiMode.COLLAPSED -> Unit
        }
    }

    Box(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .testTag("camera_zoom_overlay"),
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.88f))
                    .togetherWith(fadeOut(tween(130)) + scaleOut(targetScale = 0.90f))
            },
            label = "camera_zoom_mode",
        ) { currentMode ->
            when (currentMode) {
                CameraZoomUiMode.COLLAPSED -> {
                    val interaction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = onExpandPresets,
                        interactionSource = interaction,
                        shape = CircleShape,
                        color = Color(0xB8080D21),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
                        modifier = Modifier
                            .bouncyPress(interaction, pressedScale = 0.90f)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("camera_zoom_button"),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = formatZoomLabel(zoomRatio),
                                style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                    }
                }

                CameraZoomUiMode.PRESETS -> {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xCC080D21),
                        border = BorderStroke(1.dp, atmo.accentMint.copy(alpha = 0.50f)),
                        modifier = Modifier.testTag("camera_zoom_presets"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            presets.forEach { preset ->
                                val presetTagSuffix = formatZoomLabel(preset).replace('.', '_')
                                val isSelected = abs(zoomRatio - preset) < 0.14f
                                val chipInteraction = remember(preset) { MutableInteractionSource() }
                                val chipBg by animateColorAsState(
                                    targetValue = if (isSelected) atmo.accentMint else Color.White.copy(alpha = 0.08f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "zoom_chip_bg_$presetTagSuffix",
                                )
                                val chipFg by animateColorAsState(
                                    targetValue = if (isSelected) atmo.onAccentMint else Color.White,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "zoom_chip_fg_$presetTagSuffix",
                                )
                                Surface(
                                    onClick = { onSelectPreset(preset.coerceIn(minZoomRatio, maxZoomRatio)) },
                                    interactionSource = chipInteraction,
                                    shape = RoundedCornerShape(999.dp),
                                    color = chipBg,
                                    modifier = Modifier
                                        .bouncyPress(chipInteraction, pressedScale = 0.90f)
                                        .minimumInteractiveComponentSize()
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                                        .testTag("camera_zoom_preset_$presetTagSuffix"),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = formatZoomLabel(preset),
                                            style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                                            color = chipFg,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CameraZoomUiMode.GEAR_DIAL -> {
                    Surface(
                        onClick = onExpandPresets,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xD4080D21),
                        border = BorderStroke(1.dp, atmo.accentMint.copy(alpha = 0.62f)),
                        modifier = Modifier.testTag("camera_zoom_gear_dial"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = atmo.accentMint,
                            ) {
                                Text(
                                    text = formatZoomLabel(zoomRatio),
                                    style = MonoSmallStyle.copy(fontWeight = FontWeight.ExtraBold),
                                    color = atmo.onAccentMint,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }

                            CameraZoomGearCanvas(
                                zoomRatio = zoomRatio,
                                minZoom = minZoomRatio.coerceAtLeast(1f),
                                maxZoom = maxZoomRatio.coerceAtLeast(1.1f),
                                accentColor = atmo.accentMint,
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraZoomGearCanvas(
    zoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val baseBottomY = size.height - 2.dp.toPx()
        val pxPerTenth = 8.dp.toPx()
        val currentTenth = (zoomRatio.coerceIn(minZoom, maxZoom) * 10f)
        val minTenth = (minZoom * 10f).roundToInt()
        val maxTenth = (maxZoom * 10f).roundToInt()

        // Subtle machined gear track along the bottom
        drawRoundRect(
            color = Color.White.copy(alpha = 0.14f),
            topLeft = Offset(0f, baseBottomY - 2.dp.toPx()),
            size = Size(size.width, 2.dp.toPx()),
            cornerRadius = CornerRadius(4f, 4f),
        )

        for (step in minTenth..maxTenth) {
            val x = centerX + (step - currentTenth) * pxPerTenth
            if (x < 2f || x > size.width - 2f) continue

            val isMajor = step == 10 || step == 20 || step == 30 || step == maxTenth
            val isMedium = !isMajor && step % 5 == 0
            val distFactor = (1f - (abs(x - centerX) / (size.width * 0.52f))).coerceIn(0.22f, 1f)

            val tickHeight = when {
                isMajor -> 17.dp.toPx()
                isMedium -> 12.dp.toPx()
                else -> 7.5.dp.toPx()
            }
            val tickStroke = when {
                isMajor -> 2.1.dp.toPx()
                isMedium -> 1.5.dp.toPx()
                else -> 1.1.dp.toPx()
            }
            val tickColor = if (isMajor) {
                Color.White.copy(alpha = 0.95f * distFactor)
            } else {
                Color(0xFFCBD5E1).copy(alpha = 0.65f * distFactor)
            }

            drawLine(
                color = tickColor,
                start = Offset(x, baseBottomY - tickHeight),
                end = Offset(x, baseBottomY),
                strokeWidth = tickStroke,
                cap = StrokeCap.Round,
            )
        }

        // Fixed center indicator needle
        drawLine(
            color = accentColor,
            start = Offset(centerX, 2.dp.toPx()),
            end = Offset(centerX, baseBottomY + 1.dp.toPx()),
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
