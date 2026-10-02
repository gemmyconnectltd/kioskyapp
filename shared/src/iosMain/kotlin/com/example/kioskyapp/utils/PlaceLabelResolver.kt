package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class PlaceLabelResolver {
    actual suspend fun resolve(latitude: Double, longitude: Double): String? {
        return "Recent place"
    }
}

@Composable
actual fun rememberPlaceLabelResolver(): PlaceLabelResolver {
    return remember { PlaceLabelResolver() }
}
