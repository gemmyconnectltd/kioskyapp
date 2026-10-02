package com.example.kioskyapp.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.LocationApi
import com.example.kioskyapp.apiServices.ScreenTimeApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.AddChildResponse
import com.example.kioskyapp.utils.rememberPlaceLabelResolver

@Composable
fun ChildDetailsScreen(
    token: String,
    child: AddChildResponse?,
    isLive: Boolean = false,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUnwantedZonesClick: () -> Unit,
    onAppManagementClick: () -> Unit,
    onWebFilterClick: () -> Unit,
    onActivitiesClick: () -> Unit,
    onSetScreenTime: () -> Unit
) {
    var totalLimitMinutes by remember(child?.id) { mutableStateOf(0) }
    var usedTodayMinutes by remember(child?.id) { mutableStateOf(0) }
    var mostVisitedLabel by remember(child?.id) { mutableStateOf("Loading location...") }
    var mostVisitedMeta by remember(child?.id) { mutableStateOf("Finding frequent places") }
    val screenTimeApi = remember { ScreenTimeApi() }
    val locationApi = remember { LocationApi() }
    val placeLabelResolver = rememberPlaceLabelResolver()

    LaunchedEffect(token, child?.id) {
        val childId = child?.id ?: return@LaunchedEffect

        val ruleResult = screenTimeApi.getActiveRuleForChild(token, childId)
        if (ruleResult is Result.Success && ruleResult.data.isNotEmpty()) {
            totalLimitMinutes = ruleResult.data.first().daily_limit_min
        } else {
            totalLimitMinutes = 0
        }

        val usageResult = screenTimeApi.getTodayUsageMinutes(token, childId)
        if (usageResult is Result.Success) {
            usedTodayMinutes = usageResult.data
        } else {
            usedTodayMinutes = 0
        }

        val mostVisitedResult = locationApi.getMostVisitedLocations(token, childId, limit = 1)
        if (mostVisitedResult is Result.Success && mostVisitedResult.data.isNotEmpty()) {
            val place = mostVisitedResult.data.first()
            val resolvedLabel = placeLabelResolver.resolve(place.latitude, place.longitude)
            mostVisitedLabel = resolvedLabel ?: place.label
            mostVisitedMeta = "Visited ${place.visitCount} times"
        } else {
            mostVisitedLabel = "No frequent place yet"
            mostVisitedMeta = "More location history needed"
        }
    }

    val remainingMinutes = (totalLimitMinutes - usedTodayMinutes).coerceAtLeast(0)
    val remainingHours = remainingMinutes / 60
    val remainingMins = remainingMinutes % 60
    val timeLimitLabel = if (totalLimitMinutes > 0) {
        if (remainingHours > 0) "${remainingHours} hrs ${remainingMins} min" else "${remainingMins} min"
    } else {
        "Unlimited time"
    }

    Scaffold(
        backgroundColor = Color(0xFF00897B),
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp) {
                BottomNavigationItem(
                    selected = true,
                    onClick = onHomeClick,
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Home", tint = Color(0xFF00BFA5)) },
                    label = { Text("Home", color = Color(0xFF00BFA5)) }
                )
                BottomNavigationItem(selected = false, onClick = onMapClick, icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") }, label = { Text("Map") })
                BottomNavigationItem(selected = false, onClick = onAlertsClick, icon = { Icon(Icons.Default.FlashOn, contentDescription = "Alerts") }, label = { Text("Alerts") })
                BottomNavigationItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIos, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Child Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(child?.full_name ?: "Unknown Child", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isLive) Color(0xFF4CAF50) else Color.Gray))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isLive) "Live" else "Offline", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Time Limit Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(timeLimitLabel, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Today's time limit", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionItem(Icons.Default.Apps, "App Management", onAppManagementClick)
                ActionItem(Icons.Default.Language, "Web Filter", onWebFilterClick)
                ActionItem(Icons.Default.History, "Activities", onActivitiesClick)
                ActionItem(Icons.Default.Schedule, "Screen Time", onSetScreenTime)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Unwanted Zones and Location Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Unwanted Zones", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Get alerted when your child enters a restricted area.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onUnwantedZonesClick) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Unwanted Zone", tint = Color(0xFF00BFA5))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onUnwantedZonesClick() },
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFFFFF6F6),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE4E4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationDisabled,
                                contentDescription = null,
                                tint = Color(0xFFD84343)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Add an unwanted zone", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Choose unsafe places and alert both child and parent on entry.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onMapClick() },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color(0xFFF8FBFF),
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mostVisitedLabel, fontWeight = FontWeight.Medium)
                            Text(mostVisitedMeta, color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF00BFA5))
        Text(label, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
    }
}
