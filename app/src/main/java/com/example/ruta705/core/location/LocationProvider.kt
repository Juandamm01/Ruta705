package com.example.ruta705.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Ubicación puntual (no seguimiento continuo) usando el LocationManager del sistema,
 * sin depender de Google Play Services. Solo se usa cuando el usuario lo pide.
 */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    sealed interface Result {
        data class Found(val lat: Double, val lng: Double, val accuracyMeters: Float) : Result
        data object NoPermission : Result
        data object Disabled : Result
        data object Unavailable : Result
    }

    @SuppressLint("MissingPermission")
    suspend fun current(): Result {
        if (!hasPermission()) return Result.NoPermission
        if (!isEnabled()) return Result.Disabled
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        val fresh = withTimeoutOrNull(12_000) {
            providers.firstNotNullOfOrNull { provider -> currentFrom(manager, provider) }
        }
        val location = fresh ?: providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 5 * 60_000 }
            .maxByOrNull { it.time }
        return location?.let { Result.Found(it.latitude, it.longitude, it.accuracy) } ?: Result.Unavailable
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentFrom(manager: LocationManager, provider: String): Location? {
        if (provider == LocationManager.FUSED_PROVIDER && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            runCatching {
                LocationManagerCompat.getCurrentLocation(manager, provider, signal, Executors.newSingleThreadExecutor()) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }.onFailure { if (continuation.isActive) continuation.resume(null) }
        }
    }
}
