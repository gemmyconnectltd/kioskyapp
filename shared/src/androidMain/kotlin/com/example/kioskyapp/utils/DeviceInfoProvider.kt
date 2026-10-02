package com.example.kioskyapp.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.kioskyapp.models.ChildActivationRequest
import java.util.UUID

actual class DeviceInfoProvider(private val context: Context? = null) {
    actual fun getDeviceInfo(pinCode: String): ChildActivationRequest {
        val deviceId = context?.let {
            Settings.Secure.getString(it.contentResolver, Settings.Secure.ANDROID_ID)
        } ?: "${Build.MANUFACTURER}_${Build.MODEL}"
        
        return ChildActivationRequest(
            pin_code = pinCode,
            device_id = deviceId,
            device_name = "${Build.MANUFACTURER} ${Build.MODEL}",
            os = "Android",
            os_version = Build.VERSION.RELEASE,
            push_token = null
        )
    }
}

@Composable
actual fun rememberDeviceInfoProvider(): DeviceInfoProvider {
    val context = LocalContext.current
    return remember(context) {
        DeviceInfoProvider(context)
    }
}
