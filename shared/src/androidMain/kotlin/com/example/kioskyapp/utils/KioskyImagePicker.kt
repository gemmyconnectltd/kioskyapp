package com.example.kioskyapp.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberKioskyImagePicker(onImageSelected: (String) -> Unit): KioskyImagePicker {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it.toString()) }
    }

    return remember {
        object : KioskyImagePicker {
            override fun pickImage() {
                launcher.launch("image/*")
            }
        }
    }
}
