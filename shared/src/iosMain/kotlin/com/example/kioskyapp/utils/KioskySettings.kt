package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

actual class KioskySettings {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun putString(key: String, value: String) {
        userDefaults.setObject(value, key)
    }

    actual fun getString(key: String, defaultValue: String?): String? {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }

    actual fun clear() {
        // Not typically implemented on iOS via simple clear, but can be done if needed
        val bundleId = platform.Foundation.NSBundle.mainBundle.bundleIdentifier
        if (bundleId != null) {
            userDefaults.removePersistentDomainForName(bundleId)
        }
    }
}

@Composable
actual fun rememberKioskySettings(): KioskySettings {
    return remember { KioskySettings() }
}
