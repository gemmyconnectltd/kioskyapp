package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable
import com.example.kioskyapp.models.LocationLogRequest

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class KioskyLocationProvider {
    suspend fun getCurrentLocation(childId: String, deviceId: String): LocationLogRequest?
}

@Composable
expect fun rememberKioskyLocationProvider(): KioskyLocationProvider
