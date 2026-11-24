package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.LocalAtmospherePalette
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import kotlin.coroutines.resume

data class OsmMarkerPoint(
    val id: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val isHighlighted: Boolean = false,
)

const val DEFAULT_INDONESIA_LAT = -6.2088
const val DEFAULT_INDONESIA_LNG = 106.8456

fun configureOsmdroidSafely(context: Context) {
    runCatching {
        val basePath = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        val tileCache = File(basePath, "tiles").apply { mkdirs() }
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName.ifBlank { "com.aistudio.jadwalnih" }
        config.osmdroidBasePath = basePath
        config.osmdroidTileCache = tileCache
    }
}

fun openInGoogleMaps(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String = "",
): Boolean {
    val cleanLabel = label.trim().ifBlank { "Lokasi JadwalNih" }
    val needsNewTask = context !is android.app.Activity
    val geoUri = Uri.parse(
        "geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(cleanLabel)})",
    )
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        if (needsNewTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val openedGeo = runCatching {
        context.startActivity(geoIntent)
        true
    }.getOrDefault(false)
    if (openedGeo) return true

    val webUri = Uri.parse(
        "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude",
    )
    val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
        if (needsNewTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(webIntent)
        true
    }.getOrDefault(false)
}

suspend fun reverseGeocodeLocationName(
    context: Context,
    latitude: Double,
    longitude: Double,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        if (!Geocoder.isPresent()) return@runCatching null
        val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1) { list ->
                    if (cont.isActive) cont.resume(list.firstOrNull())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }
        address?.let { addr ->
            val parts = listOfNotNull(
                addr.subLocality?.takeIf { it.isNotBlank() } ?: addr.thoroughfare?.takeIf { it.isNotBlank() },
                addr.locality?.takeIf { it.isNotBlank() } ?: addr.subAdminArea?.takeIf { it.isNotBlank() },
                addr.adminArea?.takeIf { it.isNotBlank() },
            ).distinct()
            parts.joinToString(", ").takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}

@Composable
fun OsmInteractiveMapView(
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Double = 13.5,
    markers: List<OsmMarkerPoint> = emptyList(),
    onMapTap: ((Double, Double) -> Unit)? = null,
    onMarkerClick: ((OsmMarkerPoint) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapUnavailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        configureOsmdroidSafely(context.applicationContext)
    }

    if (mapUnavailable) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Peta interaktif siap di perangkat (${String.format(Locale.US, "%.4f, %.4f", centerLat, centerLng)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val mapView = remember {
        runCatching {
            configureOsmdroidSafely(context.applicationContext)
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isHorizontalMapRepetitionEnabled = false
                controller.setZoom(zoomLevel)
                controller.setCenter(GeoPoint(centerLat, centerLng))
            }
        }.onFailure {
            mapUnavailable = true
        }.getOrNull()
    }

    if (mapView == null) return

    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            runCatching { mapView.onResume() }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching { mapView.onResume() }
                Lifecycle.Event.ON_PAUSE -> runCatching { mapView.onPause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                InfoWindow.closeAllInfoWindowsOn(mapView)
                mapView.onPause()
                mapView.onDetach()
            }
        }
    }

    LaunchedEffect(mapView, centerLat, centerLng) {
        runCatching {
            mapView.controller.animateTo(GeoPoint(centerLat, centerLng))
        }
    }

    AndroidView(
        factory = { mapView },
        update = { mv ->
            runCatching {
                InfoWindow.closeAllInfoWindowsOn(mv)
                mv.overlays.clear()
                if (onMapTap != null) {
                    mv.overlays.add(
                        MapEventsOverlay(
                            object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    if (p != null) {
                                        onMapTap(p.latitude, p.longitude)
                                    }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            },
                        ),
                    )
                }
                markers.forEach { point ->
                    val marker = Marker(mv).apply {
                        position = GeoPoint(point.latitude, point.longitude)
                        title = point.title
                        snippet = point.subtitle
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            onMarkerClick?.invoke(point)
                            true
                        }
                    }
                    mv.overlays.add(marker)
                }
                mv.invalidate()
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerSheet(
    initialLocationName: String,
    initialLatitude: Double?,
    initialLongitude: Double?,
    onConfirmLocation: (locationName: String, latitude: Double, longitude: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }

    val atmo = LocalAtmospherePalette.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedLat by remember { mutableDoubleStateOf(initialLatitude ?: DEFAULT_INDONESIA_LAT) }
    var selectedLng by remember { mutableDoubleStateOf(initialLongitude ?: DEFAULT_INDONESIA_LNG) }
    var locationName by remember { mutableStateOf(initialLocationName.ifBlank { "Jakarta, Indonesia" }) }
    var isResolvingName by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var resolveJob by remember { mutableStateOf<Job?>(null) }

    fun handleCoordinatePicked(lat: Double, lng: Double, overrideName: String? = null) {
        selectedLat = lat
        selectedLng = lng
        resolveJob?.cancel()
        if (!overrideName.isNullOrBlank()) {
            isResolvingName = false
            locationName = overrideName
            return
        }
        isResolvingName = true
        resolveJob = scope.launch {
            val resolved = reverseGeocodeLocationName(context, lat, lng)
            if (!resolved.isNullOrBlank()) {
                locationName = resolved
            } else if (locationName.isBlank()) {
                locationName = String.format(Locale.US, "%.4f, %.4f", lat, lng)
            }
            isResolvingName = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = atmo.sheetBg,
        modifier = Modifier.testTag("map_location_picker_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pilih Titik di Peta",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Ketuk peta untuk menaruh pin atau cari kota Indonesia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BouncyIconButton(
                    onClick = {
                        haptics.tapSoft()
                        onDismiss()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup peta lokasi")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(18.dp)),
            ) {
                OsmInteractiveMapView(
                    centerLat = selectedLat,
                    centerLng = selectedLng,
                    zoomLevel = 14.0,
                    markers = listOf(
                        OsmMarkerPoint(
                            id = "picked_pin",
                            title = locationName.ifBlank { "Titik Terpilih" },
                            subtitle = String.format(Locale.US, "%.4f, %.4f", selectedLat, selectedLng),
                            latitude = selectedLat,
                            longitude = selectedLng,
                            isHighlighted = true,
                        ),
                    ),
                    onMapTap = { lat, lng ->
                        haptics.tapSoft()
                        handleCoordinatePicked(lat, lng)
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                Surface(
                    color = atmo.sheetBg.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, atmo.cardBorder),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .minimumInteractiveComponentSize()
                        .clickable {
                            haptics.tapSoft()
                            showRegionPicker = true
                        }
                        .testTag("map_picker_open_regions"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            Icons.Filled.LocationCity,
                            contentDescription = null,
                            tint = atmo.accentCobalt,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Cari Kota",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Surface(
                color = atmo.sheetCardBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = atmo.accentCoral,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (isResolvingName) {
                            "Mencari nama lokasi..."
                        } else {
                            "Koordinat: ${String.format(Locale.US, "%.5f, %.5f", selectedLat, selectedLng)}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                singleLine = true,
                label = { Text("Nama Lokasi") },
                placeholder = { Text("Misal: Stasiun Gambir / Rumah") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_picker_name_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.tapSoft()
                        openInGoogleMaps(context, selectedLat, selectedLng, locationName)
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("map_picker_open_google_maps"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Google Maps", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = {
                        haptics.tapConfirm()
                        val resolvedName = locationName.trim().ifBlank {
                            String.format(Locale.US, "%.4f, %.4f", selectedLat, selectedLng)
                        }
                        onConfirmLocation(resolvedName, selectedLat, selectedLng)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = atmo.accentCobalt,
                        contentColor = atmo.onAccentCobalt,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("map_picker_confirm_button"),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gunakan Titik", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (showRegionPicker) {
        IndonesiaRegionPickerSheet(
            onSelectRegency = { regency ->
                val lat = regency.latitude ?: selectedLat
                val lng = regency.longitude ?: selectedLng
                handleCoordinatePicked(lat, lng, overrideName = regency.displayFullName)
            },
            onDismiss = { showRegionPicker = false },
        )
    }
}
