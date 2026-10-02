package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.kioskyapp.models.InstalledAppSyncItem

actual class InstalledAppsProvider {
    actual fun getInstalledApps(deviceId: String): List<InstalledAppSyncItem> = emptyList()
}

@Composable
actual fun rememberInstalledAppsProvider(): InstalledAppsProvider {
    return remember { InstalledAppsProvider() }
}
