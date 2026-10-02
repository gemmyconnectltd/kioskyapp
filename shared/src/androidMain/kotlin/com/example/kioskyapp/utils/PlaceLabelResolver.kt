package com.example.kioskyapp.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual class PlaceLabelResolver(private val context: Context) {
    actual suspend fun resolve(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null

        val geocoder = Geocoder(context)
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    continuation.resume(addresses.firstOrNull())
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        }

        return address?.toFriendlyLabel()
    }

    private fun Address.toFriendlyLabel(): String? {
        val primary = listOfNotNull(
            featureName?.takeIf { it.isNotBlank() },
            subLocality?.takeIf { it.isNotBlank() },
            locality?.takeIf { it.isNotBlank() }
        ).distinct()

        if (primary.isNotEmpty()) {
            return primary.take(2).joinToString(", ")
        }

        return adminArea?.takeIf { it.isNotBlank() }
            ?: countryName?.takeIf { it.isNotBlank() }
    }
}

@Composable
actual fun rememberPlaceLabelResolver(): PlaceLabelResolver {
    val context = LocalContext.current
    return remember(context) { PlaceLabelResolver(context) }
}
