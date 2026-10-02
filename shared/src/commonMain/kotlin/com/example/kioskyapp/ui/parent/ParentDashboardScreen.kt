package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.BackHandler
import com.example.kioskyapp.apiServices.AuthApi
import com.example.kioskyapp.apiServices.LocationApi
import com.example.kioskyapp.apiServices.NotificationApi
import com.example.kioskyapp.apiServices.EmergencyResponse
import com.example.kioskyapp.apiServices.NotificationResponse
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.ParentUser
import com.example.kioskyapp.models.AddChildResponse
import com.example.kioskyapp.models.LocationLogResponse
import com.example.kioskyapp.navigation.AppNavigationBridge
import com.example.kioskyapp.ui.components.KioskyMap
import com.example.kioskyapp.ui.components.ChildMarker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kioskyapp.utils.isRecentlyUpdated
import com.example.kioskyapp.utils.currentTimeString

@Composable
expect fun TriggerSOSNotification(id: String, title: String, message: String)

@Composable
expect fun TriggerParentInfoNotification(id: String, title: String, message: String)


@Composable
fun ParentDashboardScreen(onLogout: () -> Unit, token: String, parentUser: ParentUser) {
    var currentSubScreen by remember { mutableStateOf("main") }
    val children = remember { mutableStateListOf<AddChildResponse>() }
    var selectedChild by remember { mutableStateOf<AddChildResponse?>(null) }
    
    val scope = rememberCoroutineScope()
    val authApi = remember { AuthApi() }
    val notificationApi = remember { NotificationApi() }
    val locationApi = remember { LocationApi() }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val childrenLocations = remember { mutableStateMapOf<String, LocationLogResponse>() }
    var syncStatus by remember { mutableStateOf("Initializing sync...") }
    val activeSOSMap = remember { mutableStateMapOf<String, EmergencyResponse>() }
    var lastSosId by remember { mutableStateOf<String?>(null) }
    var latestSOSForNotification by remember { mutableStateOf<EmergencyResponse?>(null) }
    val seenParentNotificationIds = remember { mutableStateListOf<String>() }
    var latestParentNotification by remember { mutableStateOf<NotificationResponse?>(null) }

    LaunchedEffect(token, parentUser.id) {
        while(true) {
            try {
                syncStatus = "Syncing..."
                // 1. Fetch Emergencies
                val emergencyResult = notificationApi.getUnviewedEmergencies(token, parentUser.id)
                val unviewedSos = if (emergencyResult is Result.Success) emergencyResult.data else emptyList()
                
                val currentSosChildIds = unviewedSos.map { it.child_id }.toSet()
                activeSOSMap.keys.toList().forEach { id -> 
                    if (id !in currentSosChildIds) activeSOSMap.remove(id) 
                }

                unviewedSos.forEach { sos ->
                    activeSOSMap[sos.child_id] = sos
                    if (sos.id != lastSosId) { 
                        lastSosId = sos.id
                        latestSOSForNotification = sos 
                    }
                }

                val unreadNotificationsResult = notificationApi.getUnreadNotifications(token, parentUser.id)
                if (unreadNotificationsResult is Result.Success) {
                    unreadNotificationsResult.data
                        .filter { it.type == "APP_REQUEST" }
                        .firstOrNull { it.id !in seenParentNotificationIds }
                        ?.let { notification ->
                            seenParentNotificationIds.add(notification.id)
                            latestParentNotification = notification
                        }
                }

                // 2. Fetch Children
                val childrenResult = authApi.getChildren(token, parentUser.id)
                if (childrenResult is Result.Success) {
                    val newChildren = childrenResult.data
                    if (newChildren.size != children.size || !children.all { existing -> newChildren.any { it.id == existing.id && it.is_active == existing.is_active } }) {
                        children.clear()
                        children.addAll(newChildren)
                    }

                    // 3. Update locations
                    newChildren.forEach { child ->
                        val locResult = locationApi.getLatestLocation(token, child.id)
                        val sos = activeSOSMap[child.id]

                        if (locResult is Result.Success && locResult.data != null) {
                            childrenLocations[child.id] = locResult.data!!
                        } else if (sos != null && sos.latitude != null && sos.longitude != null) {
                            childrenLocations[child.id] = LocationLogResponse(
                                id = sos.id, 
                                child_id = child.id, 
                                device_id = sos.device_id ?: "", 
                                latitude = sos.latitude, 
                                longitude = sos.longitude, 
                                accuracy_m = 0f, 
                                recorded_at = sos.created_at
                            )
                        }
                    }
                    syncStatus = "Last sync: OK"
                } else if (childrenResult is Result.Error) {
                    syncStatus = "Sync Error: ${childrenResult.exception.message}"
                }
            } catch (e: Exception) {
                syncStatus = "Sync Error"
            }
            delay(3000)
        }
    }

    if (latestSOSForNotification != null) {
        TriggerSOSNotification(id = latestSOSForNotification!!.id, title = "🚨 SOS EMERGENCY! 🚨", message = latestSOSForNotification!!.message ?: "Emergency alert received!")
        latestSOSForNotification = null
    }

    if (latestParentNotification != null) {
        TriggerParentInfoNotification(
            id = latestParentNotification!!.id,
            title = latestParentNotification!!.title,
            message = latestParentNotification!!.message
        )
        latestParentNotification = null
    }

    val pendingParentScreen = AppNavigationBridge.pendingParentScreen
    LaunchedEffect(pendingParentScreen) {
        when (pendingParentScreen) {
            "alerts" -> {
                selectedChild = null
                currentSubScreen = "alerts"
                AppNavigationBridge.consumeParentScreen()
            }
        }
    }

    val goToMain = { 
        selectedChild = null
        currentSubScreen = "main" 
    }
    val goToSettings = { currentSubScreen = "settings" }
    val goToMap = { currentSubScreen = "map" }
    val goToAlerts = { currentSubScreen = "alerts" }

    val onBack = {
        when (currentSubScreen) {
            "main" -> onLogout()
            "child_details" -> goToMain()
            "add_child" -> goToMain()
            "map" -> goToMain()
            "alerts" -> goToMain()
            "settings" -> goToMain()
            "unwanted_zones" -> currentSubScreen = "child_details"
            "app_management" -> currentSubScreen = "child_details"
            "internet" -> currentSubScreen = "child_details"
            "activities" -> currentSubScreen = "child_details"
            "browser_filter" -> currentSubScreen = "internet"
            "screen_time" -> currentSubScreen = "child_details"
            else -> goToMain()
        }
    }
    
    BackHandler(enabled = true, onBack = onBack)

    when (currentSubScreen) {
        "add_child" -> AddChildScreen(
            onSave = { childData ->
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    val result = authApi.addChild(token, childData)
                    isLoading = false
                    if (result is Result.Success) { 
                        children.add(result.data)
                        goToMain() 
                    } else if (result is Result.Error) { 
                        errorMessage = result.exception.message 
                    }
                }
            },
            onBack = goToMain, isLoading = isLoading, errorMessage = errorMessage
        )
        "child_details" -> {
            val loc = childrenLocations[selectedChild?.id ?: ""]
            val isLive = loc != null && isRecentlyUpdated(loc.recorded_at)
            ChildDetailsScreen(
                token = token,
                child = selectedChild, 
                isLive = isLive,
                onBack = goToMain, 
                onHomeClick = goToMain, 
                onMapClick = goToMap, 
                onAlertsClick = goToAlerts, 
                onSettingsClick = goToSettings, 
                onUnwantedZonesClick = { currentSubScreen = "unwanted_zones" }, 
                onAppManagementClick = { currentSubScreen = "app_management" }, 
                onWebFilterClick = { currentSubScreen = "internet" }, 
                onActivitiesClick = { currentSubScreen = "activities" }, 
                onSetScreenTime = { currentSubScreen = "screen_time" }
            )
        }
        "unwanted_zones" -> UnwantedZonesScreen(
            childName = selectedChild?.full_name ?: "Your child",
            onBack = { currentSubScreen = "child_details" },
            onHomeClick = goToMain,
            onMapClick = goToMap,
            onAlertsClick = goToAlerts,
            onSettingsClick = goToSettings
        )
        "settings" -> SettingsScreen(onBack = goToMain, onHomeClick = goToMain, onMapClick = goToMap, onAlertsClick = goToAlerts, onActivatePro = { })
        "app_management" -> AppManagementScreen(
            token = token,
            childId = selectedChild?.id,
            onBack = { currentSubScreen = "child_details" },
            onHomeClick = goToMain,
            onMapClick = goToMap,
            onAlertsClick = goToAlerts,
            onSettingsClick = goToSettings
        )
        "internet" -> InternetScreen(
            token = token,
            childId = selectedChild?.id ?: "",
            parentId = parentUser.id,
            onBack = { currentSubScreen = "child_details" }, 
            onHomeClick = goToMain, 
            onMapClick = goToMap, 
            onSettingsClick = goToSettings, 
            onBrowsersClick = { currentSubScreen = "browser_filter" }
        )
        "activities" -> ActivitiesScreen(
            token = token,
            childId = selectedChild?.id,
            onBack = { currentSubScreen = "child_details" },
            onHomeClick = goToMain,
            onMapClick = goToMap,
            onAlertsClick = goToAlerts,
            onSettingsClick = goToSettings,
            onSetScreenTime = { currentSubScreen = "screen_time" }
        )
        "browser_filter" -> BrowserFilterScreen(
            token = token,
            childId = selectedChild?.id ?: "",
            parentId = parentUser.id,
            onBack = { currentSubScreen = "internet" }, 
            onHomeClick = goToMain, 
            onMapClick = goToMap, 
            onAlertsClick = goToAlerts, 
            onSettingsClick = goToSettings
        )
        "screen_time" -> ScreenTimeScreen(
            token = token,
            parentId = parentUser.id,
            childId = selectedChild?.id ?: "",
            onBack = { currentSubScreen = "child_details" }
        )
        "map" -> {
            MapScreen(
                token = token, 
                parentId = parentUser.id, 
                childId = selectedChild?.id, 
                childName = selectedChild?.full_name,
                children = children,
                childrenLocations = childrenLocations,
                activeSOSMap = activeSOSMap,
                onBack = goToMain, 
                onHomeClick = goToMain, 
                onAlertsClick = goToAlerts, 
                onSettingsClick = goToSettings
            )
        }
        "alerts" -> AlertsScreen(token = token, parentId = parentUser.id, childId = selectedChild?.id, onBack = goToMain, onSettingsClick = goToSettings, onMapClick = goToMap)
        else -> MainDashboardContent(children = children, childrenLocations = childrenLocations, activeSOSMap = activeSOSMap, onAddChild = { currentSubScreen = "add_child" }, onChildClick = { child -> if(child.is_active == true) { selectedChild = child; currentSubScreen = "child_details" } }, onSettingsClick = goToSettings, onMapClick = goToMap, onAlertsClick = goToAlerts, isLoading = isLoading, errorMessage = errorMessage, syncStatus = syncStatus)
    }
}

@Composable
fun MainDashboardContent(
    children: List<AddChildResponse>, childrenLocations: Map<String, LocationLogResponse>, activeSOSMap: Map<String, EmergencyResponse>,
    onAddChild: () -> Unit, onChildClick: (AddChildResponse) -> Unit, onSettingsClick: () -> Unit, onMapClick: () -> Unit, onAlertsClick: () -> Unit,
    isLoading: Boolean, errorMessage: String?, syncStatus: String
) {
    val allMarkers = children.mapNotNull { child ->
        val loc = childrenLocations[child.id]
        if (loc != null) {
            ChildMarker(
                id = child.id, 
                name = child.full_name, 
                latitude = loc.latitude, 
                longitude = loc.longitude, 
                isSOS = activeSOSMap.containsKey(child.id),
                isLive = isRecentlyUpdated(loc.recorded_at)
            )
        } else null
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp, modifier = Modifier.height(72.dp)) {
                BottomNavigationItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.GridView, "Home", tint = Color(0xFF00BFA5)) }, label = { Text("Home", color = Color(0xFF00BFA5)) })
                BottomNavigationItem(selected = false, onClick = onMapClick, icon = { Icon(Icons.Default.LocationOn, "Map") }, label = { Text("Map") })
                BottomNavigationItem(selected = false, onClick = onAlertsClick, icon = { Icon(Icons.Default.FlashOn, "Alerts") }, label = { Text("Alerts") })
                BottomNavigationItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, "Settings") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).background(Color(0xFFF5F9FF))) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val primaryMarker = allMarkers.find { it.isSOS } ?: allMarkers.firstOrNull()
                KioskyMap(
                    modifier = Modifier.fillMaxSize(),
                    latitude = primaryMarker?.latitude ?: 0.0,
                    longitude = primaryMarker?.longitude ?: 0.0,
                    title = primaryMarker?.name ?: "All Children",
                    isSOS = primaryMarker?.isSOS ?: false,
                    markers = allMarkers
                )
                if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF00BFA5)) }
            }

            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Color.White).padding(vertical = 24.dp)) {
                if (errorMessage != null) Text(errorMessage, color = Color.Red, modifier = Modifier.padding(horizontal = 24.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Your children", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onAddChild, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.AddCircle, null, tint = Color(0xFF00BFA5)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(children) { child ->
                        val isSOS = activeSOSMap.containsKey(child.id)
                        val loc = childrenLocations[child.id]
                        val isOnline = loc != null && isRecentlyUpdated(loc.recorded_at)
                        val isActive = child.is_active ?: false
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onChildClick(child) }) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(if (isSOS) Color.Red.copy(alpha = 0.1f) else Color(0xFFF5F5F5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person, 
                                        null, 
                                        tint = if (isSOS) Color.Red else if (isActive && isOnline) Color(0xFF00BFA5) else if (isActive) Color.Gray else Color.LightGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    if (isSOS) {
                                        CircularProgressIndicator(modifier = Modifier.size(60.dp), color = Color.Red, strokeWidth = 2.dp)
                                    }
                                }
                                
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(2.dp)
                                            .background(if (isOnline) Color(0xFF00BFA5) else Color.LightGray, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(child.full_name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isSOS) Color.Red else Color.Black)
                            
                            if (isActive) {
                                Text(if (isOnline) "Live" else "Offline", fontSize = 10.sp, color = if (isOnline) Color(0xFF00BFA5) else Color.Gray)
                            } else {
                                Text("Code: ${child.pincode ?: "..."}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF008080))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = syncStatus,
                    fontSize = 10.sp,
                    color = if (syncStatus.contains("Error")) Color.Red else Color.Gray,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
