package com.roadalert.cameroun.detection

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationCaptureManager(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager = context.getSystemService(
        Context.LOCATION_SERVICE
    ) as LocationManager

    companion object {
        // 5s pour Fused + 5s pour natif = 10s total SAD ✅
        private const val FUSED_TIMEOUT_MS = 5_000L
        private const val NATIVE_TIMEOUT_MS = 5_000L
    }

    @SuppressLint("MissingPermission")
    suspend fun captureLocation():
            com.roadalert.cameroun.detection.LocationResult {

        return try {

            // ── Tentative 1 — FusedLocationProvider corrigé ──
            val fused = withTimeoutOrNull(FUSED_TIMEOUT_MS) {
                getFusedLocationSuspend()
            }

            if (fused != null) {
                return com.roadalert.cameroun
                    .detection.LocationResult.Success(
                        latitude = fused.latitude,
                        longitude = fused.longitude,
                        isApproximate = false
                    )
            }

            // ── Tentative 2 — LocationManager natif Android ──
            val native = withTimeoutOrNull(NATIVE_TIMEOUT_MS) {
                getNativeLocationSuspend()
            }

            if (native != null) {
                return com.roadalert.cameroun
                    .detection.LocationResult.Success(
                        latitude = native.latitude,
                        longitude = native.longitude,
                        isApproximate = false
                    )
            }

            // ── Tentative 3 — Dernière position connue ────────
            val lastKnown = getLastKnownLocation()

            if (lastKnown != null) {
                return com.roadalert.cameroun
                    .detection.LocationResult.Success(
                        latitude = lastKnown.latitude,
                        longitude = lastKnown.longitude,
                        isApproximate = true
                    )
            }

            // Aucune position disponible
            com.roadalert.cameroun
                .detection.LocationResult.Unavailable

        } catch (e: SecurityException) {
            com.roadalert.cameroun
                .detection.LocationResult.Unavailable
        } catch (e: Exception) {
            com.roadalert.cameroun
                .detection.LocationResult.Unavailable
        }
    }

    // ── FusedLocationProvider — paramètres corrigés ───────

    @SuppressLint("MissingPermission")
    private suspend fun getFusedLocationSuspend():
            Location? =
        suspendCancellableCoroutine { continuation ->

            val locationRequest = LocationRequest
                .Builder(
                    // BALANCED = plus tolérant sur Android Go
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    1000L
                )
                .setMaxUpdates(1)
                // FALSE = ne pas attendre position ultra précise
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(
                    result: LocationResult
                ) {
                    fusedClient.removeLocationUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(result.lastLocation)
                    }
                }

                override fun onLocationAvailability(
                    availability: LocationAvailability
                ) {
                    if (!availability.isLocationAvailable) {
                        fusedClient.removeLocationUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }

            try {
                fusedClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
                continuation.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                }
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

    // ── LocationManager natif Android — fallback ──────────

    @SuppressLint("MissingPermission")
    private suspend fun getNativeLocationSuspend():
            Location? =
        suspendCancellableCoroutine { continuation ->

            val provider = when {
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ) -> LocationManager.GPS_PROVIDER

                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                ) -> LocationManager.NETWORK_PROVIDER

                else -> null
            }

            if (provider == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(
                    location: Location
                ) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                @Deprecated("Deprecated in API 29")
                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?
                ) {}

                override fun onProviderEnabled(
                    provider: String
                ) {}

                override fun onProviderDisabled(
                    provider: String
                ) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            try {
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

    // ── Dernière position connue — tous providers ─────────

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            try {
                val location = locationManager
                    .getLastKnownLocation(provider)
                    ?: continue

                if (bestLocation == null ||
                    location.accuracy < bestLocation.accuracy) {
                    bestLocation = location
                }
            } catch (e: SecurityException) {
                continue
            }
        }
        return bestLocation
    }
}