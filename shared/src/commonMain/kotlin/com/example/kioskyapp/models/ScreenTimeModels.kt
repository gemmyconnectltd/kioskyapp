package com.example.kioskyapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeRuleRequest(
    val parent_id: String,
    val child_id: String,
    val daily_limit_min: Int,
    val unlock_after_min: Int = 30,
    val is_active: Boolean = true,
    val device_id: String? = null
)

@Serializable
data class ScreenTimeRuleResponse(
    val id: String,
    val parent_id: String,
    val child_id: String,
    val daily_limit_min: Int,
    val unlock_after_min: Int? = 30,
    val is_active: Boolean,
    val created_at: String
)

@Serializable
data class ScreenTimeUsageResponse(
    val id: String,
    val child_id: String,
    val device_id: String,
    val total_minutes: Int,
    val usage_date: String
)
