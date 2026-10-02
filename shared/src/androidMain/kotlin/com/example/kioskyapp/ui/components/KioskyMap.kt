package com.example.kioskyapp.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private val childMarkerHues = listOf(
    BitmapDescriptorFactory.HUE_AZURE,
    BitmapDescriptorFactory.HUE_BLUE,
    BitmapDescriptorFactory.HUE_CYAN,
    BitmapDescriptorFactory.HUE_GREEN,
    BitmapDescriptorFactory.HUE_MAGENTA,
    BitmapDescriptorFactory.HUE_ORANGE,
    BitmapDescriptorFactory.HUE_ROSE,
    BitmapDescriptorFactory.HUE_VIOLET,
    BitmapDescriptorFactory.HUE_YELLOW
)

private fun markerHueForChild(childId: String): Float {
    val safeHash = childId.hashCode().toLong() and 0x7fffffff
    return childMarkerHues[(safeHash % childMarkerHues.size).toInt()]
}

@Composable
actual fun KioskyMap(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    title: String?,
    isSOS: Boolean,
    markers: List<ChildMarker>
) {
    // Default to Rwanda (Kigali)
    val rwandaCenter = LatLng(-1.9441, 30.0619)
    
    // If the provided focus is 0,0, use Rwanda center
    val centerLocation = if (latitude != 0.0 || longitude != 0.0) LatLng(latitude, longitude) else rwandaCenter
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLocation, if (latitude == 0.0) 7f else 15f)
    }

    // Update camera position when focus location changes
    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 || longitude != 0.0) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), if (isSOS) 17f else 15f),
                durationMs = 1000
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        markers.forEach { child ->
            // Draw marker if it has any non-zero coordinate, 
            // or even if it's 0,0 (though unlikely for a real location)
            // We only skip if BOTH are exactly 0.0 and we have reason to believe it's uninitialized
            if (child.latitude != 0.0 || child.longitude != 0.0) {
                Marker(
                    state = MarkerState(position = LatLng(child.latitude, child.longitude)),
                    title = child.name,
                    snippet = if (child.isSOS) "🚨 SOS ACTIVE!" else if (child.isLive) "Live location" else "Last seen location",
                    icon = BitmapDescriptorFactory.defaultMarker(markerHueForChild(child.id)),
                    alpha = if (child.isLive || child.isSOS) 1f else 0.75f,
                    zIndex = if (child.isSOS) 2f else 1f
                )
            }
        }
    }
}
