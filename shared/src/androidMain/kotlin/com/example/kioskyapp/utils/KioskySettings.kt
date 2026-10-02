package com.example.kioskyapp.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class KioskySettings(private val context: Context) {
    private val prefs = context.getSharedPreferences("kiosky_prefs", Context.MODE_PRIVATE)

    actual fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    actual fun clear() {
        prefs.edit().clear().apply()
    }
}

@Composable
actual fun rememberKioskySettings(): KioskySettings {
    val context = LocalContext.current
    return remember(context) { KioskySettings(context) }
}
