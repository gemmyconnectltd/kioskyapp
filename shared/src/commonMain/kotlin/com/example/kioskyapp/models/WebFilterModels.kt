package com.example.kioskyapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
    WEBSITE,
    CATEGORY,
    KEYWORD
}

@Serializable
data class ContentFilterDTO(
    val id: String? = null,
    val parent_id: String,
    val child_id: String,
    val filter_type: FilterType,
    val value: String,
    val is_blocked: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class CreateContentFilterRequest(
    val parent_id: String,
    val child_id: String,
    val filter_type: FilterType,
    val value: String,
    val is_blocked: Boolean = true
)

@Serializable
data class UpdateContentFilterRequest(
    val value: String? = null,
    val is_blocked: Boolean? = null
)
