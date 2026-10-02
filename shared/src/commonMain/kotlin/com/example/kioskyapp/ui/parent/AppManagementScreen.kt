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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.AppManagementApi
import com.example.kioskyapp.apiServices.DeviceApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.AppRequestDto
import com.example.kioskyapp.models.DeviceDTO
import com.example.kioskyapp.models.ManagedAppDto
import kotlinx.coroutines.launch

@Composable
fun AppManagementScreen(
    token: String,
    childId: String?,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appApi = remember { AppManagementApi() }
    val deviceApi = remember { DeviceApi() }

    var selectedDevice by remember(childId) { mutableStateOf<DeviceDTO?>(null) }
    var apps by remember(childId) { mutableStateOf<List<ManagedAppDto>>(emptyList()) }
    var pendingRequests by remember(childId) { mutableStateOf<List<AppRequestDto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember(childId) { mutableStateOf(true) }
    var errorMessage by remember(childId) { mutableStateOf<String?>(null) }
    var busyAppId by remember { mutableStateOf<String?>(null) }
    var busyRequestId by remember { mutableStateOf<String?>(null) }
    var limitDialogApp by remember { mutableStateOf<ManagedAppDto?>(null) }
    var limitInput by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }

    suspend fun loadData(showLoading: Boolean = true) {
        val safeChildId = childId
        if (safeChildId.isNullOrBlank()) {
            selectedDevice = null
            apps = emptyList()
            pendingRequests = emptyList()
            errorMessage = "No child selected"
            isLoading = false
            return
        }

        if (showLoading) {
            isLoading = true
        }
        errorMessage = null

        when (val devicesResult = deviceApi.getDevicesByChildId(token, safeChildId)) {
            is Result.Success -> {
                selectedDevice = devicesResult.data.firstOrNull { it.is_active } ?: devicesResult.data.firstOrNull()
            }
            is Result.Error -> {
                selectedDevice = null
                apps = emptyList()
                pendingRequests = emptyList()
                errorMessage = devicesResult.exception.message ?: "Failed to load device"
                isLoading = false
                return
            }
        }

        val currentDevice = selectedDevice
        if (currentDevice?.id != null) {
            when (val appsResult = appApi.getAppsByDevice(token, currentDevice.id)) {
                is Result.Success -> apps = appsResult.data.sortedBy { it.app_name.lowercase() }
                is Result.Error -> {
                    apps = emptyList()
                    errorMessage = appsResult.exception.message ?: "Failed to load apps"
                }
            }
        } else {
            apps = emptyList()
        }

        when (val requestsResult = appApi.getPendingRequests(token, safeChildId)) {
            is Result.Success -> pendingRequests = requestsResult.data
            is Result.Error -> {
                pendingRequests = emptyList()
                if (errorMessage == null) {
                    errorMessage = requestsResult.exception.message ?: "Failed to load requests"
                }
            }
        }

        isLoading = false
    }

    LaunchedEffect(token, childId) {
        loadData()
    }

    val filteredApps = remember(apps, searchQuery, statusFilter) {
        apps.filter { app ->
            searchQuery.isBlank() ||
                app.app_name.contains(searchQuery, ignoreCase = true) ||
                app.package_name.contains(searchQuery, ignoreCase = true) ||
                (app.category?.contains(searchQuery, ignoreCase = true) == true)
        }.filter { app ->
            when (statusFilter) {
                "Blocked" -> app.is_blocked
                "Allowed" -> !app.is_blocked
                else -> true
            }
        }.sortedWith(compareByDescending<ManagedAppDto> { it.is_blocked }.thenBy { it.app_name.lowercase() })
    }

    val blockedCount = apps.count { it.is_blocked }
    val limitedCount = apps.count { (it.daily_limit_min ?: 0) > 0 }

    Scaffold(
        backgroundColor = Color(0xFFF5F9FF),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("App management", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Back", tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { loadData(showLoading = false) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF00BFA5))
                    }
                },
                backgroundColor = Color.White,
                elevation = 0.dp
            )
        },
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, elevation = 8.dp) {
                BottomNavigationItem(
                    selected = false,
                    onClick = onHomeClick,
                    icon = { Icon(Icons.Default.GridView, contentDescription = null, tint = Color(0xFF00BFA5)) },
                    label = { Text("Home", color = Color(0xFF00BFA5)) }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onMapClick,
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Map") }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onAlertsClick,
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                    label = { Text("Alerts") }
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Child device", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                selectedDevice?.device_name ?: "No connected device yet",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                        }
                        Text(
                            selectedDevice?.os ?: "",
                            color = Color(0xFF00BFA5),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (selectedDevice == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Open the child app on the phone so kioskyApp can sync installed apps.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Installed",
                    value = apps.size.toString(),
                    icon = Icons.Default.Apps,
                    accent = Color(0xFF00BFA5)
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Blocked",
                    value = blockedCount.toString(),
                    icon = Icons.Default.Lock,
                    accent = Color(0xFFD32F2F)
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Timed",
                    value = limitedCount.toString(),
                    icon = Icons.Default.Timer,
                    accent = Color(0xFFFF9800)
                )
            }

            if (pendingRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Pending app requests", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                pendingRequests.forEach { request ->
                    AppRequestCard(
                        request = request,
                        isBusy = busyRequestId == request.id,
                        onApprove = {
                            scope.launch {
                                busyRequestId = request.id
                                when (val approveResult = appApi.approveRequest(token, request.id)) {
                                    is Result.Success -> {
                                        val existingApp = apps.firstOrNull {
                                            it.package_name.equals(request.package_name, ignoreCase = true)
                                        }
                                        if (existingApp != null && existingApp.is_blocked) {
                                            appApi.unblockApp(token, existingApp.id)
                                        }
                                        loadData(showLoading = false)
                                        snackbarHostState.showSnackbar("${request.app_name} approved")
                                    }
                                    is Result.Error -> {
                                        snackbarHostState.showSnackbar(
                                            approveResult.exception.message ?: "Could not approve request"
                                        )
                                    }
                                }
                                busyRequestId = null
                            }
                        },
                        onDeny = {
                            scope.launch {
                                busyRequestId = request.id
                                when (val denyResult = appApi.denyRequest(token, request.id)) {
                                    is Result.Success -> {
                                        loadData(showLoading = false)
                                        snackbarHostState.showSnackbar("${request.app_name} denied")
                                    }
                                    is Result.Error -> {
                                        snackbarHostState.showSnackbar(
                                            denyResult.exception.message ?: "Could not deny request"
                                        )
                                    }
                                }
                                busyRequestId = null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Manage installed apps", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Review the child device app list, then switch between blocked and allowed apps below.",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton(
                    title = "All",
                    isSelected = statusFilter == "All",
                    onClick = { statusFilter = "All" }
                )
                FilterButton(
                    title = "Blocked",
                    isSelected = statusFilter == "Blocked",
                    onClick = { statusFilter = "Blocked" }
                )
                FilterButton(
                    title = "Allowed",
                    isSelected = statusFilter == "Allowed",
                    onClick = { statusFilter = "Allowed" }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by app name or package") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF00BFA5),
                    focusedLabelColor = Color(0xFF00BFA5)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00BFA5))
                }
            } else if (errorMessage != null && apps.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White,
                    elevation = 0.dp
                ) {
                    Text(
                        text = errorMessage ?: "Something went wrong",
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (filteredApps.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White,
                    elevation = 0.dp
                ) {
                    Text(
                        text = if (apps.isEmpty()) {
                            "No apps have been synced from the child phone yet."
                        } else {
                            "No apps match your search."
                        },
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                filteredApps.forEach { app ->
                    ManagedAppCard(
                        app = app,
                        isBusy = busyAppId == app.id,
                        onToggleBlocked = {
                            scope.launch {
                                busyAppId = app.id
                                val result = if (app.is_blocked) {
                                    appApi.unblockApp(token, app.id)
                                } else {
                                    appApi.blockApp(token, app.id)
                                }

                                when (result) {
                                    is Result.Success -> {
                                        apps = apps.map { if (it.id == app.id) result.data else it }
                                        snackbarHostState.showSnackbar(
                                            if (result.data.is_blocked) {
                                                "${app.app_name} blocked"
                                            } else {
                                                "${app.app_name} unblocked"
                                            }
                                        )
                                    }
                                    is Result.Error -> {
                                        snackbarHostState.showSnackbar(
                                            result.exception.message ?: "Could not update app"
                                        )
                                    }
                                }
                                busyAppId = null
                            }
                        },
                        onSetLimit = {
                            limitDialogApp = app
                            limitInput = (app.daily_limit_min ?: 0).takeIf { it > 0 }?.toString().orEmpty()
                        },
                        onRemoveLimit = {
                            scope.launch {
                                busyAppId = app.id
                                when (val result = appApi.removeDailyLimit(token, app.id)) {
                                    is Result.Success -> {
                                        apps = apps.map { if (it.id == app.id) result.data else it }
                                        snackbarHostState.showSnackbar("Removed time limit for ${app.app_name}")
                                    }
                                    is Result.Error -> {
                                        snackbarHostState.showSnackbar(
                                            result.exception.message ?: "Could not remove time limit"
                                        )
                                    }
                                }
                                busyAppId = null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (limitDialogApp != null) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { limitDialogApp = null },
            title = { Text("Set app time") },
            text = {
                Column {
                    Text(
                        "Choose how many minutes ${limitDialogApp?.app_name ?: "this app"} can be used today.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { value ->
                            limitInput = value.filter { it.isDigit() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Minutes per day") },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF00BFA5),
                            focusedLabelColor = Color(0xFF00BFA5)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedApp = limitDialogApp ?: return@TextButton
                        val minutes = limitInput.toIntOrNull()
                        if (minutes == null || minutes <= 0) {
                            scope.launch { snackbarHostState.showSnackbar("Enter a valid number of minutes") }
                            return@TextButton
                        }

                        scope.launch {
                            busyAppId = selectedApp.id
                            when (val result = appApi.setDailyLimit(token, selectedApp.id, minutes)) {
                                is Result.Success -> {
                                    apps = apps.map { if (it.id == selectedApp.id) result.data else it }
                                    snackbarHostState.showSnackbar("Saved ${selectedApp.app_name} time limit")
                                }
                                is Result.Error -> {
                                    snackbarHostState.showSnackbar(
                                        result.exception.message ?: "Could not save time limit"
                                    )
                                }
                            }
                            busyAppId = null
                            limitDialogApp = null
                        }
                    }
                ) {
                    Text("Save", color = Color(0xFF00BFA5))
                }
            },
            dismissButton = {
                TextButton(onClick = { limitDialogApp = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            backgroundColor = Color.White
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = accent)
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF263238))
            Text(title, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AppRequestCard(
    request: AppRequestDto,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.app_name, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    Text(request.package_name, color = Color.Gray, fontSize = 12.sp)
                }
                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFFF9800))
            }

            if (!request.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(request.reason, color = Color(0xFF455A64), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5))
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = onDeny,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Deny", color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
private fun ManagedAppCard(
    app: ManagedAppDto,
    isBusy: Boolean,
    onToggleBlocked: () -> Unit,
    onSetLimit: () -> Unit,
    onRemoveLimit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.app_name, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    Text(
                        app.category ?: app.package_name,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = !app.is_blocked,
                    onCheckedChange = { if (!isBusy) onToggleBlocked() },
                    enabled = !isBusy,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00BFA5),
                        uncheckedThumbColor = Color(0xFFD32F2F)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (app.is_blocked) "Blocked" else "Allowed",
                        color = if (app.is_blocked) Color(0xFFD32F2F) else Color(0xFF00BFA5),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        app.daily_limit_min?.let { "$it min/day" } ?: "No daily time limit",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF00BFA5),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSetLimit,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF00BFA5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set time", color = Color(0xFF00BFA5))
                }

                if ((app.daily_limit_min ?: 0) > 0) {
                    TextButton(onClick = onRemoveLimit, enabled = !isBusy) {
                        Text("Remove limit", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    if (isSelected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5))
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(50)) {
            Text(title, color = Color.Gray)
        }
    }
}
