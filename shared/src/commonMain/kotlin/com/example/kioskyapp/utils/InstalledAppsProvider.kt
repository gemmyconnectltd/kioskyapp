package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable
import com.example.kioskyapp.models.InstalledAppSyncItem

expect class InstalledAppsProvider {
    fun getInstalledApps(deviceId: String): List<InstalledAppSyncItem>
}

@Composable
expect fun rememberInstalledAppsProvider(): InstalledAppsProvider
