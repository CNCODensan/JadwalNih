package com.example.domain

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ResolvedLocation(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val isFromGps: Boolean = true,
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveCurrentLocation(context: Context): ResolvedLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )

        var bestLocation: Location? = providers
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }

        // If stale or missing, try a brief current location request on API 30+
        if (bestLocation == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val activeProvider = providers.firstOrNull { provider ->
                runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
            }
            if (activeProvider != null) {
                bestLocation = withTimeoutOrNull(4500L) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        runCatching {
                            manager.getCurrentLocation(
                                activeProvider,
                                null,
                                context.mainExecutor,
                            ) { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            }
        }

        val loc = bestLocation ?: return@withContext null
        val readable = reverseGeocodeAddress(context, loc.latitude, loc.longitude)
            ?: String.format(Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)

        ResolvedLocation(
            locationName = readable,
            latitude = loc.latitude,
            longitude = loc.longitude,
            isFromGps = true,
        )
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeAddress(context: Context, lat: Double, lng: Double): String? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var resultList: List<Address>? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                geocoder.getFromLocation(lat, lng, 1) { list ->
                    resultList = list
                    latch.countDown()
                }
                latch.await(2500, java.util.concurrent.TimeUnit.MILLISECONDS)
                resultList
            } else {
                geocoder.getFromLocation(lat, lng, 1)
            }
            val addr = addresses?.firstOrNull() ?: return@runCatching null
            formatReadablePlace(addr)
        }.getOrNull()
    }

    internal fun formatReadablePlace(addr: Address): String? {
        val parts = listOfNotNull(
            addr.featureName?.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } },
            addr.thoroughfare?.takeIf { it.isNotBlank() },
            addr.subLocality?.takeIf { it.isNotBlank() },
            addr.locality?.takeIf { it.isNotBlank() } ?: addr.subAdminArea?.takeIf { it.isNotBlank() },
            addr.adminArea?.takeIf { it.isNotBlank() },
        ).distinct()
        return if (parts.isNotEmpty()) {
            parts.take(3).joinToString(", ")
        } else {
            addr.getAddressLine(0)?.takeIf { it.isNotBlank() }
        }
    }
}
