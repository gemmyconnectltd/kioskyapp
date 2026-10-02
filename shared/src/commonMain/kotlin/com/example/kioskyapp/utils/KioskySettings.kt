package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable

expect class KioskySettings {
    fun putString(key: String, value: String)
    fun getString(key: String, defaultValue: String? = null): String?
    fun remove(key: String)
    fun clear()
}

@Composable
expect fun rememberKioskySettings(): KioskySettings
