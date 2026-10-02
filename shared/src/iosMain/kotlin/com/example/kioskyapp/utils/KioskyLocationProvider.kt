package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.kioskyapp.models.LocationLogRequest

// Placeholder for iOS - does not fetch real location
actual class KioskyLocationProvider {
    actual suspend fun getCurrentLocation(childId: String, deviceId: String): LocationLogRequest? {
        return null // No-op for iOS for now
    }
}

@Composable
actual fun rememberKioskyLocationProvider(): KioskyLocationProvider {
    return remember { KioskyLocationProvider() }
}
