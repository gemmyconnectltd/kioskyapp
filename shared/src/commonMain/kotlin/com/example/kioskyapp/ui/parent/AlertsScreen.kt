package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.EmergencyResponse
import com.example.kioskyapp.apiServices.NotificationApi
import com.example.kioskyapp.apiServices.NotificationResponse
import com.example.kioskyapp.data.Result
import kotlinx.coroutines.launch

@Composable
fun AlertsScreen(
    token: String,
    parentId: String,
    childId: String? = null,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val notificationApi = remember { NotificationApi() }

    var emergencies by remember { mutableStateOf<List<EmergencyResponse>>(emptyList()) }
    var appRequestNotifications by remember { mutableStateOf<List<NotificationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val fetchAlerts = {
        scope.launch {
            isLoading = true

            val emergencyResult = notificationApi.getUnviewedEmergencies(token, parentId)
            if (emergencyResult is Result.Success) {
                emergencies = if (childId != null) {
                    emergencyResult.data.filter { it.child_id == childId }
                } else {
                    emergencyResult.data
                }
            }

            val notificationResult = notificationApi.getUnreadNotifications(token, parentId)
            if (notificationResult is Result.Success) {
                val filtered = notificationResult.data
                    .filter { it.type == "APP_REQUEST" }
                    .filter { childId == null || it.child_id == childId }
                appRequestNotifications = filtered
            }

            isLoading = false
        }
    }

    LaunchedEffect(childId) {
        fetchAlerts()
    }

    val hasAnyAlerts = appRequestNotifications.isNotEmpty() || emergencies.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts", fontWeight = FontWeight.Bold) },
                backgroundColor = Color.White,
                contentColor = Color.Black,
                elevation = 0.dp
            )
        },
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp) {
                BottomNavigationItem(selected = false, onClick = onBack, icon = { Icon(Icons.Default.GridView, "Home") })
                BottomNavigationItem(selected = false, onClick = onMapClick, icon = { Icon(Icons.Default.LocationOn, "Map") })
                BottomNavigationItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.FlashOn, "Alerts", tint = Color(0xFF00BFA5)) })
                BottomNavigationItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, "Settings") })
            }
        }
    ) { padding ->
        when {
            isLoading && !hasAnyAlerts -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00BFA5))
                }
            }
            !hasAnyAlerts -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Text("No active alerts", color = Color.Gray)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (appRequestNotifications.isNotEmpty()) {
                        item {
                            Text(
                                "App requests",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF263238)
                            )
                        }

                        items(appRequestNotifications) { notification ->
                            AppRequestAlertCard(
                                notification = notification,
                                onReviewed = {
                                    scope.launch {
                                        val result = notificationApi.markNotificationAsRead(token, notification.id)
                                        if (result is Result.Success) {
                                            fetchAlerts()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (emergencies.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Emergency alerts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF263238)
                            )
                        }

                        items(emergencies) { sos ->
                            EmergencyAlertCard(
                                sos = sos,
                                onAttended = {
                                    scope.launch {
                                        val success = notificationApi.markEmergencyAsViewed(token, sos.id)
                                        if (success is Result.Success) {
                                            fetchAlerts()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRequestAlertCard(notification: NotificationResponse, onReviewed: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00BFA5).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00BFA5))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(notification.title, fontWeight = FontWeight.Bold, color = Color(0xFF00BFA5), fontSize = 16.sp)
                    Text(notification.created_at, fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(notification.message, fontSize = 14.sp, color = Color(0xFF455A64))
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onReviewed,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("MARK AS REVIEWED", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmergencyAlertCard(sos: EmergencyResponse, onAttended: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color.Red)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SOS EMERGENCY", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 16.sp)
                    Text(sos.created_at, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(sos.message ?: "Child has pressed the SOS button!", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAttended,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("MARK AS ATTENDED", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
