package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable

expect class PlaceLabelResolver {
    suspend fun resolve(latitude: Double, longitude: Double): String?
}

@Composable
expect fun rememberPlaceLabelResolver(): PlaceLabelResolver
