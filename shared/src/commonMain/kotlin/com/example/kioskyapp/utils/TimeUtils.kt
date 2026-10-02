package com.example.kioskyapp.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun isRecentlyUpdated(timestamp: String?): Boolean {
    if (timestamp == null) return false
    return try {
        // Handle ISO-8601 strings from backend (e.g. 2026-03-12T08:11:13.000Z)
        val lastUpdate = Instant.parse(timestamp)
        val now = Clock.System.now()
        val difference = now - lastUpdate
        difference.inWholeMinutes < 5
    } catch (e: Exception) {
        false
    }
}

fun currentTimeString(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
}
