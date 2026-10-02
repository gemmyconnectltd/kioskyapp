package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberKioskyImagePicker(onImageSelected: (String) -> Unit): KioskyImagePicker

interface KioskyImagePicker {
    fun pickImage()
}
