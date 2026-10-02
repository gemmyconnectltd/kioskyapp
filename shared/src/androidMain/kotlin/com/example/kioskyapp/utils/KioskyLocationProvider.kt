package com.example.kioskyapp.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.kioskyapp.models.LocationLogRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class KioskyLocationProvider(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(childId: String, deviceId: String): LocationLogRequest? = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: KioskyLocationProvider: Requesting fresh location...")
            // Try to get fresh location
            val locationTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            var location = Tasks.await(locationTask)
            
            // Fallback to last known location if fresh location is unavailable
            if (location == null) {
                println("DEBUG: KioskyLocationProvider: Fresh location NULL, trying last known...")
                val lastLocationTask = fusedLocationClient.lastLocation
                location = Tasks.await(lastLocationTask)
            }
            
            if (location != null) {
                println("DEBUG: KioskyLocationProvider: SUCCESS! Lat=${location.latitude}, Lon=${location.longitude}")
                LocationLogRequest(
                    child_id = childId,
                    device_id = deviceId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy_m = location.accuracy
                )
            } else {
                println("DEBUG: KioskyLocationProvider: FAILED - All location sources returned null")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: KioskyLocationProvider: EXCEPTION: ${e.message}")
            null
        }
    }
}

@Composable
actual fun rememberKioskyLocationProvider(): KioskyLocationProvider {
    val context = LocalContext.current
    return remember(context) {
        KioskyLocationProvider(context)
    }
}
