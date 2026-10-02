package com.example.kioskyapp.ui.kid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kioskyapp.ui.kid.dashboard.KidMainDashboard
import com.example.kioskyapp.ui.kid.dashboard.RequestAppUsageScreen
import com.example.kioskyapp.apiServices.AppManagementApi
import com.example.kioskyapp.apiServices.LocationApi
import com.example.kioskyapp.apiServices.ScreenTimeApi
import com.example.kioskyapp.utils.rememberKioskyLocationProvider
import com.example.kioskyapp.utils.rememberDeviceInfoProvider
import com.example.kioskyapp.utils.rememberInstalledAppsProvider
import com.example.kioskyapp.utils.rememberKioskySettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Person
import com.example.kioskyapp.data.Result

@Composable
fun KidDashboardScreen(onLogout: () -> Unit) {
    var currentScreen by remember { mutableStateOf("splash") }
    var childToken by remember { mutableStateOf<String?>(null) }
    var childId by remember { mutableStateOf<String?>(null) }
    var deviceUuid by remember { mutableStateOf<String?>(null) }
    var parentId by remember { mutableStateOf<String?>(null) }
    var childName by remember { mutableStateOf("Your child") }
    var currentLatitude by remember { mutableStateOf(0.0) }
    var currentLongitude by remember { mutableStateOf(0.0) }
    var trackingStatus by remember { mutableStateOf<String?>(null) }

    // Screen Time State
    var totalLimitMinutes by remember { mutableStateOf(0) }
    var usedMinutes by remember { mutableStateOf(0) }
    var usedTimeMs by remember { mutableStateOf(0L) }

    val scope = rememberCoroutineScope()
    val locationApi = remember { LocationApi() }
    val screenTimeApi = remember { ScreenTimeApi() }
    val appManagementApi = remember { AppManagementApi() }
    val locationProvider = rememberKioskyLocationProvider()
    val deviceInfoProvider = rememberDeviceInfoProvider()
    val installedAppsProvider = rememberInstalledAppsProvider()
    val settings = rememberKioskySettings()
    val clearChildSession = {
        settings.remove("child_token")
        settings.remove("child_id")
        settings.remove("device_id")
        settings.remove("parent_id")
        settings.remove("child_name")
        settings.remove("screen_time_used_live_ms")
        // Intentionally DO NOT clear other keys (e.g. today's screen-time counters)
    }

    // Load persisted session on startup
    LaunchedEffect(Unit) {
        val savedToken = settings.getString("child_token")
        val savedChildId = settings.getString("child_id")
        val savedDeviceUuid = settings.getString("device_id")
        val savedParentId = settings.getString("parent_id")
        val savedName = settings.getString("child_name")

        if (savedToken != null && savedChildId != null) {
            childToken = savedToken
            childId = savedChildId
            deviceUuid = savedDeviceUuid
            parentId = savedParentId
            childName = savedName ?: "Your child"
            currentScreen = "continue_or_switch"
        } else {
            currentScreen = "authentication"
        }
    }

    // Background Location Tracking Effect
    LaunchedEffect(childToken, childId, deviceUuid) {
        if (childToken != null && childId != null) {
            while (true) {
                try {
                    val deviceInfo = deviceInfoProvider.getDeviceInfo("")
                    val activeDeviceId = deviceUuid ?: deviceInfo.device_id
                    
                    val locationLog = locationProvider.getCurrentLocation(childId!!, activeDeviceId)
                    
                    if (locationLog != null) {
                        currentLatitude = locationLog.latitude
                        currentLongitude = locationLog.longitude
                        val result = locationApi.sendLocationLog(childToken!!, locationLog)
                        if (result is Result.Success) {
                            trackingStatus = "Tracking: SUCCESS (${locationLog.latitude.toString().take(7)}, ${locationLog.longitude.toString().take(7)})"
                        } else if (result is Result.Error) {
                            val msg = result.exception.message ?: "Unknown Error"
                            trackingStatus = "Tracking: FAILED - $msg"
                        }
                    } else {
                        trackingStatus = "Tracking: FAILED - Location Unavailable"
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: "Unknown Exception"
                    trackingStatus = "Tracking: ERROR - $msg"
                }
                delay(7000)
            }
        }
    }

    // Screen Time Data Sync Effect
    LaunchedEffect(childToken, childId) {
        if (childToken != null && childId != null) {
            while (true) {
                // 1. Get Limit
                val ruleResult = screenTimeApi.getActiveRuleForChild(childToken!!, childId!!)
                if (ruleResult is Result.Success && ruleResult.data.isNotEmpty()) {
                    totalLimitMinutes = ruleResult.data.first().daily_limit_min
                }

                // 2. Get Usage
                val usageResult = screenTimeApi.getTodayUsageMinutes(childToken!!, childId!!)
                if (usageResult is Result.Success) {
                    usedMinutes = usageResult.data
                    val fallbackMs = usageResult.data * 60_000L
                    if (usedTimeMs < fallbackMs) {
                        usedTimeMs = fallbackMs
                    }
                }
                
                delay(30000) // Update every 30 seconds
            }
        }
    }

    LaunchedEffect(childToken, deviceUuid) {
        if (childToken != null && deviceUuid != null) {
            while (true) {
                try {
                    val installedApps = installedAppsProvider.getInstalledApps(deviceUuid!!)
                    if (installedApps.isNotEmpty()) {
                        appManagementApi.syncInstalledApps(childToken!!, installedApps)
                    }
                } catch (_: Exception) {
                }
                delay(60000)
            }
        }
    }

    LaunchedEffect(childId, deviceUuid) {
        if (childId != null && deviceUuid != null) {
            while (true) {
                val liveMs = settings.getString("screen_time_used_live_ms", "0")?.toLongOrNull() ?: 0L
                usedTimeMs = liveMs
                delay(1000)
            }
        }
    }

    val showPersistentTopBar = currentScreen == "main_home" || currentScreen == "request_app_usage"

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (showPersistentTopBar) {
                    TopAppBar(
                        title = { Text("Hi $childName!", fontWeight = FontWeight.Bold) },
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF008080),
                        elevation = 0.dp,
                        actions = {
                            IconButton(onClick = { /* Handle Notifications */ }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                    )
                }
            },
            backgroundColor = Color(0xFFF5F5F5)
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (currentScreen) {
                    "splash" -> KidSplashScreen(
                        onTimeout = { currentScreen = "continue_or_switch" }
                    )
                    "continue_or_switch" -> {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF008080)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(48.dp))
                                }
                                Text("Welcome Back!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("Logged in as: $childName", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { currentScreen = "main_home" },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                                ) {
                                    Text("Continue as $childName", color = Color(0xFF008080), fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        clearChildSession()
                                        childToken = null
                                        childId = null
                                        deviceUuid = null
                                        parentId = null
                                        childName = "Your child"
                                        currentScreen = "authentication"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Switch Child / Add New Child")
                                }
                            }
                        }
                    }
                    "authentication" -> KidAuthenticationPage(
                        onBack = onLogout,
                        onSuccess = { token, id, pId, name, dId ->
                            childToken = token
                            childId = id
                            parentId = pId
                            childName = name
                            deviceUuid = dId
                            currentScreen = "main_home"
                        }
                    )
                    "main_home" -> KidMainDashboard(
                        childId = childId,
                        childName = childName,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        trackingStatus = trackingStatus,
                        totalLimitMinutes = totalLimitMinutes,
                        usedMinutes = usedMinutes,
                        usedTimeMs = usedTimeMs,
                        onNavigateToRequestAppUsage = { currentScreen = "request_app_usage" },
                        onNavigateToRestrictedAreas = { currentScreen = "restricted_areas" },
                        onLogout = {
                            clearChildSession()
                            onLogout()
                        }
                    )
                    "restricted_areas" -> RestrictedAreasScreen(
                        childName = childName,
                        onBack = { currentScreen = "main_home" }
                    )
                    "request_app_usage" -> RequestAppUsageScreen(
                        childToken = childToken,
                        onBack = { currentScreen = "main_home" },
                        onSent = { currentScreen = "main_home" }
                    )
                }
            }
        }

        KioskySOSButton(
            childToken = childToken,
            childId = childId,
            deviceUuid = deviceUuid,
            parentId = parentId,
            childName = childName
        )
    }
}
