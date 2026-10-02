package com.example.kioskyapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class ChildMarker(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isSOS: Boolean = false,
    val isLive: Boolean = true
)

@Composable
expect fun KioskyMap(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    title: String? = null,
    isSOS: Boolean = false,
    markers: List<ChildMarker> = emptyList()
)
