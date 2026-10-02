package com.example.kioskyapp.utils

import androidx.compose.runtime.Composable
import com.example.kioskyapp.models.ChildActivationRequest

expect class DeviceInfoProvider {
    fun getDeviceInfo(pinCode: String): ChildActivationRequest
}

@Composable
expect fun rememberDeviceInfoProvider(): DeviceInfoProvider
