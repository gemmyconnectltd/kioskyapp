package com.example.kioskyapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ManagedAppDto(
    val id: String,
    val device_id: String,
    val app_name: String,
    val package_name: String,
    val category: String? = null,
    val icon_url: String? = null,
    val is_blocked: Boolean = false,
    val daily_limit_min: Int? = null,
    val installed_at: String? = null
)

@Serializable
data class InstalledAppSyncItem(
    val device_id: String,
    val app_name: String,
    val package_name: String,
    val category: String? = null,
    val icon_url: String? = null
)

@Serializable
data class InstalledAppsSyncRequest(
    val apps: List<InstalledAppSyncItem>
)

@Serializable
data class AppLimitRequest(
    val minutes: Int
)

@Serializable
data class AppRequestCreateRequest(
    val app_name: String,
    val package_name: String,
    val reason: String? = null
)

@Serializable
data class AppRequestDto(
    val id: String,
    val child_id: String,
    val device_id: String,
    val app_name: String,
    val package_name: String,
    val reason: String? = null,
    val status: String = "PENDING",
    val requested_at: String? = null,
    val responded_at: String? = null
)

@Serializable
data class AppUsageSyncItem(
    val package_name: String,
    val app_name: String,
    val usage_date: String,
    val total_minutes: Int
)

@Serializable
data class AppUsageSyncRequest(
    val usages: List<AppUsageSyncItem>
)

@Serializable
data class AppUsageBreakdownDto(
    val date: String,
    val total_minutes: Int
)

@Serializable
data class AppUsageTopAppDto(
    val package_name: String,
    val app_name: String,
    val total_minutes: Int
)

@Serializable
data class AppUsageSummaryDto(
    val period: String,
    val total_minutes: Int,
    val total_apps_used: Int,
    val top_apps: List<AppUsageTopAppDto>,
    val daily_breakdown: List<AppUsageBreakdownDto>
)
