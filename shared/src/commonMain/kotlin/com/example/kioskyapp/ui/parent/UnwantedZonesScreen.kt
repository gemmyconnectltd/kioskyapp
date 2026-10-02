package com.example.kioskyapp.ui.parent

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
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class UnwantedZoneDraft(
    val name: String,
    val note: String
)

@Composable
fun UnwantedZonesScreen(
    childName: String,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val zones = remember { mutableStateListOf<UnwantedZoneDraft>() }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unwanted Zones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Back")
                    }
                },
                backgroundColor = Color.White,
                contentColor = Color.Black,
                elevation = 0.dp
            )
        },
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp) {
                BottomNavigationItem(selected = false, onClick = onHomeClick, icon = { Icon(Icons.Default.GridView, "Home") })
                BottomNavigationItem(selected = false, onClick = onMapClick, icon = { Icon(Icons.Default.LocationOn, "Map") })
                BottomNavigationItem(selected = false, onClick = onAlertsClick, icon = { Icon(Icons.Default.FlashOn, "Alerts") })
                BottomNavigationItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, "Settings") })
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
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFFFFF5F5),
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
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
                                    "Restrict unsafe areas and receive an instant entry alert.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD84343)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Unwanted Zone", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (zones.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = Color.White,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFCEBEC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationDisabled,
                                    contentDescription = null,
                                    tint = Color(0xFFD84343)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No unwanted zones yet", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Start by adding a restricted place like a bar, unsafe street, or any area your child should avoid.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Current restricted zones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF263238)
                    )
                }

                items(zones) { zone ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color.White,
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFCEBEC)),
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
                                    Text(zone.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            if (zone.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(zone.note, color = Color(0xFF455A64), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddUnwantedZoneDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, note ->
                zones.add(
                    UnwantedZoneDraft(
                        name = name,
                        note = note
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddUnwantedZoneDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, note: String) -> Unit
) {
    var zoneName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Unwanted Zone", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This first UI step lets us define the zone details. Map pin selection and backend saving come next.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Zone name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Why this area is restricted") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(zoneName.trim(), note.trim()) },
                enabled = zoneName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD84343))
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
