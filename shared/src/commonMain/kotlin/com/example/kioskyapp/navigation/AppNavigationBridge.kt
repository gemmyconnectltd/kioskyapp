package com.example.kioskyapp.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppNavigationBridge {
    var pendingParentScreen by mutableStateOf<String?>(null)
        private set

    fun openParentScreen(screen: String) {
        pendingParentScreen = screen
    }

    fun consumeParentScreen(): String? {
        val destination = pendingParentScreen
        pendingParentScreen = null
        return destination
    }
}
