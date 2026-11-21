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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
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

private const val MAX_REKAP_CAMERA_ZOOM = 3.7f

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

private fun formatZoomLabel(ratio: Float): String {
    val roundedTenth = (ratio * 10f).toInt()
    return if (roundedTenth % 10 == 0) {
        "${roundedTenth / 10}x"
    } else {
        String.format(Locale.US, "%.1fx", ratio)
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
                cameraUnavailableMessage = null

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val executor = ContextCompat.getMainExecutor(context)
                var boundProvider: ProcessCameraProvider? = null

                val listener = Runnable {
                    runCatching {
                        val provider = cameraProviderFuture.get()
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
                                }
                            }
                        },
                )
            }

            // Left-side compact vertical zoom slider overlay (up to 3.7x)
            val safeMaxZoom = minOf(controller.maxZoomRatio, MAX_REKAP_CAMERA_ZOOM)
            if (controller.isCameraReady && safeMaxZoom > 1.08f) {
                var lastZoomDetent by remember { mutableIntStateOf((controller.zoomRatio * 2f).toInt()) }
                Surface(
                    color = Color(0xA8080D21),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .testTag("camera_zoom_overlay"),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = formatZoomLabel(safeMaxZoom),
                            style = MonoSmallStyle.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFCBD5E1),
                        )

                        Slider(
                            value = controller.zoomRatio.coerceIn(controller.minZoomRatio, safeMaxZoom),
                            onValueChange = { targetZoom ->
                                val detent = (targetZoom * 2f).toInt()
                                if (detent != lastZoomDetent) {
                                    lastZoomDetent = detent
                                    haptics.tapTick()
                                }
                                controller.setZoom(targetZoom)
                            },
                            valueRange = controller.minZoomRatio..safeMaxZoom,
                            colors = SliderDefaults.colors(
                                thumbColor = atmo.accentMint,
                                activeTrackColor = atmo.accentMint,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                            ),
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = 270f
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(
                                        Constraints(
                                            minWidth = constraints.minHeight,
                                            maxWidth = constraints.maxHeight,
                                            minHeight = constraints.minWidth,
                                            maxHeight = constraints.maxWidth,
                                        ),
                                    )
                                    layout(placeable.height, placeable.width) {
                                        placeable.place(0, placeable.width)
                                    }
                                }
                                .width(140.dp)
                                .height(34.dp)
                                .testTag("camera_zoom_slider"),
                        )

                        Text(
                            text = formatZoomLabel(controller.zoomRatio),
                            style = MonoSmallStyle.copy(fontWeight = FontWeight.Bold),
                            color = atmo.accentMint,
                        )
                    }
                }
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

            // Compact live time badge pill at bottom-center inside viewfinder
            Surface(
                color = Color(0xA8080D21),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
