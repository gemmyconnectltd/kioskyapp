package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.models.AddChildResponse
import com.example.kioskyapp.apiServices.EmergencyResponse
import com.example.kioskyapp.models.LocationLogResponse
import com.example.kioskyapp.apiServices.LocationApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.ui.components.KioskyMap
import com.example.kioskyapp.ui.components.ChildMarker
import kotlinx.coroutines.delay
import com.example.kioskyapp.utils.isRecentlyUpdated
import com.example.kioskyapp.utils.rememberPlaceLabelResolver

@Composable
fun MapScreen(
    token: String,
    parentId: String,
    childId: String?, 
    childName: String?, 
    children: List<AddChildResponse>,
    childrenLocations: Map<String, LocationLogResponse>,
    activeSOSMap: Map<String, EmergencyResponse>,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val locationApi = remember { LocationApi() }
    var locationHistory by remember { mutableStateOf<List<LocationLogResponse>>(emptyList()) }
    val placeLabelResolver = rememberPlaceLabelResolver()
    val resolvedPlaces = remember { mutableStateMapOf<String, String>() }

    // Derived markers for all children
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

    // List of markers to show (filter to ONE if childId is provided, else show ALL)
    val markersToShow = if (childId != null) {
        allMarkers.filter { it.id == childId }
    } else {
        allMarkers
    }

    // Centering logic: 
    // If specific child focused -> focus on their last loc
    // If global view -> focus on SOS child if exists, else first online child, else first marker, else Rwanda
    val focusMarker = if (childId != null) {
        allMarkers.find { it.id == childId }
    } else {
        allMarkers.find { it.isSOS } ?: allMarkers.find { it.isLive } ?: allMarkers.firstOrNull()
    }

    val finalLat = focusMarker?.latitude ?: 0.0
    val finalLon = focusMarker?.longitude ?: 0.0

    // Only pull history for the specific child
    LaunchedEffect(childId) {
        if (childId != null) {
            while(true) {
                val historyResult = locationApi.getLocationHistory(token, childId)
                if (historyResult is Result.Success) {
                    locationHistory = historyResult.data
                }
                delay(5000)
            }
        }
    }

    LaunchedEffect(locationHistory) {
        locationHistory.take(5).forEach { location ->
            if (!resolvedPlaces.containsKey(location.id)) {
                val resolved = placeLabelResolver.resolve(location.latitude, location.longitude)
                resolvedPlaces[location.id] = resolved ?: "Recent place"
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp) {
                BottomNavigationItem(selected = false, onClick = onHomeClick, icon = { Icon(Icons.Default.GridView, "Home") })
                BottomNavigationItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.LocationOn, "Map", tint = Color(0xFF00BFA5)) })
                BottomNavigationItem(selected = false, onClick = onAlertsClick, icon = { Icon(Icons.Default.FlashOn, "Alerts") })
                BottomNavigationItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, "Settings") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val isSelectedSOS = if (childId != null) activeSOSMap.containsKey(childId) else false
                val selectedLoc = if (childId != null) childrenLocations[childId] else null
                
                KioskyMap(
                    modifier = Modifier.fillMaxSize(),
                    latitude = finalLat,
                    longitude = finalLon,
                    title = childName ?: "All Children",
                    isSOS = isSelectedSOS,
                    markers = markersToShow
                )

                if (childId != null && allMarkers.none { it.id == childId }) {
                    Box(modifier = Modifier.align(Alignment.Center).background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                        Text("No recent location for $childName", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }

                IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.White.copy(alpha = 0.7f), CircleShape)) {
                    Icon(Icons.Default.ArrowBackIos, "Back", tint = Color.Black)
                }

                if (selectedLoc != null || isSelectedSOS) {
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = if (isSelectedSOS) Color(0xFFFFEBEE) else Color.White,
                        elevation = 4.dp
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSelectedSOS) Color.Red.copy(alpha = 0.1f) else Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isSelectedSOS) Icons.Default.Warning else Icons.Default.Person, 
                                    null, 
                                    tint = if (isSelectedSOS) Color.Red else Color(0xFF00BFA5),
                                    modifier = Modifier.size(24.dp)
                                )
                                if (isSelectedSOS) CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color.Red, strokeWidth = 2.dp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isSelectedSOS) "EMERGENCY: $childName" else "$childName's Location", 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isSelectedSOS) Color.Red else Color.Black
                                )
                                Text(
                                    if (selectedLoc != null) "Last updated: ${selectedLoc.recorded_at}" else "Status: Active SOS",
                                    color = Color.Gray, 
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            if (childId != null) {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Color.White).padding(24.dp)) {
                    Text("Recent Visited Places - $childName", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (locationHistory.isEmpty()) {
                        Text("No recent history available.", color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                            items(locationHistory.take(5)) { location ->
                                RecentPlaceItem(
                                    title = resolvedPlaces[location.id] ?: "Resolving place...",
                                    time = location.recorded_at
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentPlaceItem(title: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00BFA5), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(time, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
