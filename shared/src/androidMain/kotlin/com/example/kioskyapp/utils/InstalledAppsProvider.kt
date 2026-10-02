package com.example.kioskyapp.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.kioskyapp.models.InstalledAppSyncItem

actual class InstalledAppsProvider(private val context: Context) {

    actual fun getInstalledApps(deviceId: String): List<InstalledAppSyncItem> {
        val packageManager = context.packageManager
        val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

        return installedApps
            .asSequence()
            .filterNot { it.packageName == context.packageName }
            .filter { appInfo -> packageManager.getLaunchIntentForPackage(appInfo.packageName) != null }
            .mapNotNull { appInfo ->
                buildInstalledApp(deviceId, packageManager, appInfo)
            }
            .sortedBy { it.app_name.lowercase() }
            .toList()
    }

    private fun buildInstalledApp(
        deviceId: String,
        packageManager: PackageManager,
        appInfo: ApplicationInfo
    ): InstalledAppSyncItem? {
        return runCatching {
            InstalledAppSyncItem(
                device_id = deviceId,
                app_name = packageManager.getApplicationLabel(appInfo).toString(),
                package_name = appInfo.packageName,
                category = appCategoryName(appInfo.category),
                icon_url = null
            )
        }.getOrNull()
    }

    private fun appCategoryName(category: Int): String? {
        return when (category) {
            ApplicationInfo.CATEGORY_AUDIO -> "Audio"
            ApplicationInfo.CATEGORY_GAME -> "Games"
            ApplicationInfo.CATEGORY_IMAGE -> "Photography"
            ApplicationInfo.CATEGORY_MAPS -> "Maps"
            ApplicationInfo.CATEGORY_NEWS -> "News"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_VIDEO -> "Video"
            ApplicationInfo.CATEGORY_UNDEFINED -> null
            else -> null
        }
    }
}

@Composable
actual fun rememberInstalledAppsProvider(): InstalledAppsProvider {
    val context = LocalContext.current
    return remember(context) {
        InstalledAppsProvider(context)
    }
}
