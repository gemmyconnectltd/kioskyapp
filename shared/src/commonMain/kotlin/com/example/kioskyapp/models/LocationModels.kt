package com.example.kioskyapp.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationLogRequest(
    val child_id: String,
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy_m: Float? = null
)

@Serializable
data class LocationLogResponse(
    val id: String,
    val child_id: String,
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy_m: Float? = null,
    val recorded_at: String // Using String for timestamp as it comes in ISO format
)

@Serializable
data class MostVisitedLocation(
    val latitude: Double,
    val longitude: Double,
    val visitCount: Int,
    val label: String
)
