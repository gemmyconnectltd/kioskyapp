package com.example.kioskyapp.models

import kotlinx.serialization.Serializable

@Serializable
data class RestrictedZoneDTO(
    val id: String? = null,
    val parent_id: String,
    val child_id: String,
    val zone_name: String,
    val reason: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius_m: Float? = null,
    val is_active: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class CreateRestrictedZoneRequest(
    val parent_id: String,
    val child_id: String,
    val zone_name: String,
    val reason: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius_m: Float? = null,
    val is_active: Boolean = true
)
