package com.example.kioskyapp.ui.kid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RestrictedAreaUi(
    val name: String,
    val reason: String
)

@Composable
fun RestrictedAreasScreen(
    childName: String,
    onBack: () -> Unit
) {
    val restrictedAreas = remember {
        listOf(
            RestrictedAreaUi(
                name = "Downtown Arcade",
                reason = "This area is off-limits during school days."
            ),
            RestrictedAreaUi(
                name = "Riverside Bar Street",
                reason = "Parent marked this place as unsafe."
            ),
            RestrictedAreaUi(
                name = "Old Market Block",
                reason = "Too crowded and difficult to supervise."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restricted Areas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Back")
                    }
                },
                backgroundColor = Color.White,
                contentColor = Color(0xFF1E293B),
                elevation = 0.dp
            )
        },
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp) {
                BottomNavigationItem(
                    selected = false,
                    onClick = onBack,
                    icon = { Icon(Icons.Default.GridView, "Home") }
                )
                BottomNavigationItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.LocationDisabled, "Restricted Areas", tint = Color(0xFFD84343)) }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.FlashOn, "Alerts") }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Settings, "Settings") }
                )
            }
        },
        backgroundColor = Color(0xFFF7F8FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Color(0xFFFFF5F5),
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationDisabled,
                                    contentDescription = null,
                                    tint = Color(0xFFD84343)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(childName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "These are the places your parent asked you to avoid.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            "Stay away from these places to avoid getting a warning and alerting your parent.",
                            color = Color(0xFF455A64),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Current restricted places",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }

            items(restrictedAreas) { area ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = Color.White,
                    elevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFCEBEC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFD84343)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(area.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(area.reason, color = Color(0xFF546E7A), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
