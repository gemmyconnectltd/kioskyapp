package com.example.kioskyapp.ui.kid.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.kioskyapp.ui.components.KioskyMap
import com.example.kioskyapp.ui.components.ChildMarker
import kotlinx.coroutines.delay

@Composable
fun KidMainDashboard(
    childId: String?,
    childName: String?,
    latitude: Double,
    longitude: Double,
    trackingStatus: String? = null,
    totalLimitMinutes: Int = 0,
    usedMinutes: Int = 0,
    usedTimeMs: Long = 0L,
    onNavigateToRequestAppUsage: () -> Unit,
    onNavigateToRestrictedAreas: () -> Unit,
    onLogout: () -> Unit
) {
    val totalLimitSeconds = totalLimitMinutes * 60
    var remainingSeconds by remember(totalLimitMinutes, usedMinutes, usedTimeMs) {
        val preciseUsedSeconds = (usedTimeMs / 1000L).toInt()
        val fallbackUsedSeconds = usedMinutes * 60
        mutableStateOf((totalLimitSeconds - maxOf(preciseUsedSeconds, fallbackUsedSeconds)).coerceAtLeast(0))
    }

    LaunchedEffect(totalLimitMinutes, usedMinutes) {
        while (totalLimitSeconds > 0 && remainingSeconds > 0) {
            delay(1000)
            if (remainingSeconds > 0) {
                remainingSeconds--
            }
        }
    }

    val remainingMinutes = remainingSeconds / 60
    val displayHours = remainingMinutes / 60
    val displayMins = remainingMinutes % 60
    val displaySecs = remainingSeconds % 60
    val timeRemainingString = if (totalLimitMinutes > 0) {
        if (displayHours > 0) "${displayHours}h ${displayMins}m ${displaySecs}s" else "${displayMins}m ${displaySecs}s"
    } else {
        "Unlimited Time"
    }
    val progress = if (totalLimitSeconds > 0) {
        ((totalLimitSeconds - remainingSeconds).toFloat() / totalLimitSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val appUsageList = listOf(
        AppUsage("YouTube Kids", "1h 20m", 0.8f, Color(0xFFFF0000)),
        AppUsage("Roblox", "45m", 0.5f, Color(0xFF2196F3)),
        AppUsage("Duolingo", "10m", 0.1f, Color(0xFF4CAF50)),
        AppUsage("Minecraft", "0m", 0.0f, Color(0xFF795548))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Self Location Map
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 8.dp)
            ) {
                KioskyMap(
                    latitude = latitude,
                    longitude = longitude,
                    title = "My Location",
                    markers = listOfNotNull(
                        if (childId != null && childName != null && latitude != 0.0) 
                            ChildMarker(childId, childName, latitude, longitude) 
                        else null
                    )
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Time Status Card
            Card(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = if (remainingMinutes < 15 && totalLimitMinutes > 0) Color(0xFFD32F2F) else Color(0xFF008080),
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Time remaining today", color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(timeRemainingString, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        color = Color.White,
                        backgroundColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    if (totalLimitMinutes > 0 && remainingMinutes == 0) {
                        Text("Time is up! Please put the phone away.", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White,
                elevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFFCEBEC), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationDisabled,
                            contentDescription = null,
                            tint = Color(0xFFD84343)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restricted Areas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF263238)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "See the places your parent asked you to avoid.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    TextButton(onClick = onNavigateToRestrictedAreas) {
                        Text("Open", color = Color(0xFFD84343), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Request Access Button
            Button(
                onClick = onNavigateToRequestAppUsage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF008080))
            ) {
                Text(text = "Request App Access", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text(
                text = "App Usage",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        items(appUsageList) { app ->
            AppUsageItem(app)
        }

        item {
            // Encouragement Text
            Text(
                text = if (remainingMinutes > 0 || totalLimitMinutes == 0) 
                    "Keep up the great work! You've used your screen time wisely today."
                else "You've reached your limit for today. Great job staying balanced!",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = TextAlign.Center
            )
        }
        
        item {
            if (trackingStatus != null) {
                Text(
                    text = "📡 $trackingStatus",
                    color = if (trackingStatus.contains("SUCCESS")) Color(0xFF008080) else Color.Red,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            TextButton(onClick = onLogout) {
                Text("Logout", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AppUsageItem(app: AppUsage) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(app.color, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = app.progress,
                    color = Color(0xFF008080),
                    backgroundColor = Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = app.time, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

data class AppUsage(val name: String, val time: String, val progress: Float, val color: Color)
