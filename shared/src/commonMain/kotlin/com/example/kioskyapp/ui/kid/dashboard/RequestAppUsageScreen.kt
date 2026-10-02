package com.example.kioskyapp.ui.kid.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.AppManagementApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.ManagedAppDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RequestAppUsageScreen(
    childToken: String?,
    onBack: () -> Unit,
    onSent: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val appManagementApi = remember { AppManagementApi() }

    var apps by remember { mutableStateOf<List<ManagedAppDto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var customAppName by remember { mutableStateOf("") }
    var customPackageName by remember { mutableStateOf("") }
    var customReason by remember { mutableStateOf("") }
    var requestedPackages by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var sendingPackage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadApps() {
        val safeToken = childToken
        if (safeToken.isNullOrBlank()) {
            isLoading = false
            errorMessage = "Child login is required before loading app access."
            return
        }

        when (val result = appManagementApi.getCurrentChildDeviceApps(safeToken)) {
            is Result.Success -> {
                apps = result.data.sortedWith(compareByDescending<ManagedAppDto> { it.is_blocked }.thenBy { it.app_name.lowercase() })
                errorMessage = null
            }
            is Result.Error -> {
                apps = emptyList()
                errorMessage = result.exception.message ?: "Could not load app list."
            }
        }

        when (val requestResult = appManagementApi.getCurrentChildRequests(safeToken)) {
            is Result.Success -> {
                requestedPackages = requestResult.data
                    .filter { it.status == "PENDING" }
                    .map { it.package_name.lowercase() }
                    .toSet()
            }
            is Result.Error -> {
                if (errorMessage == null) {
                    errorMessage = requestResult.exception.message ?: "Could not load request statuses."
                }
            }
        }
        isLoading = false
    }

    suspend fun sendRequest(appName: String, packageName: String, reason: String) {
        val safeToken = childToken
        if (safeToken.isNullOrBlank()) {
            errorMessage = "Child login is required before sending a request."
            return
        }

        isSending = true
        sendingPackage = packageName.lowercase()
        when (
            val result = appManagementApi.createAppRequest(
                token = safeToken,
                appName = appName,
                packageName = packageName,
                reason = reason
            )
        ) {
            is Result.Success -> {
                requestedPackages = requestedPackages + packageName.lowercase()
                showSuccess = true
                errorMessage = null
            }
            is Result.Error -> {
                errorMessage = result.exception.message ?: "Could not send the request."
            }
        }
        isSending = false
        sendingPackage = null
    }

    LaunchedEffect(childToken) {
        loadApps()
    }

    val filteredApps = remember(apps, searchQuery) {
        apps.filter { app ->
            searchQuery.isBlank() ||
                app.app_name.contains(searchQuery, ignoreCase = true) ||
                app.package_name.contains(searchQuery, ignoreCase = true)
        }
    }

    val blockedApps = filteredApps.filter { it.is_blocked }
    val allowedApps = filteredApps.filterNot { it.is_blocked }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "App Access",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "See which apps are already allowed and request access to blocked ones.",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChildSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Allowed",
                    value = allowedApps.size.toString(),
                    color = Color(0xFF00BFA5)
                )
                ChildSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Blocked",
                    value = blockedApps.size.toString(),
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search apps") },
                placeholder = { Text("Find an app quickly") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF008080),
                    focusedLabelColor = Color(0xFF008080)
                )
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFD32F2F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF008080))
                }
            } else {
                Text(
                    text = "Blocked apps",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (blockedApps.isEmpty()) {
                    EmptyAppsCard("No blocked apps right now.")
                } else {
                    blockedApps.forEach { app ->
                        AppAccessCard(
                            app = app,
                            isAllowed = false,
                            isRequested = app.package_name.lowercase() in requestedPackages,
                            isSending = sendingPackage == app.package_name.lowercase(),
                            onRequest = {
                                scope.launch {
                                    sendRequest(
                                        appName = app.app_name,
                                        packageName = app.package_name,
                                        reason = "Please allow access to ${app.app_name}."
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Allowed apps",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (allowedApps.isEmpty()) {
                    EmptyAppsCard("No allowed apps were found yet.")
                } else {
                    allowedApps.forEach { app ->
                        AppAccessCard(
                            app = app,
                            isAllowed = true,
                            isRequested = false,
                            isSending = false,
                            onRequest = { }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White,
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Request another app",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF263238)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If the app is not listed yet, send a custom request here.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customAppName,
                        onValueChange = { customAppName = it },
                        label = { Text("App name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF008080),
                            focusedLabelColor = Color(0xFF008080)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customPackageName,
                        onValueChange = { customPackageName = it },
                        label = { Text("Package name (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF008080),
                            focusedLabelColor = Color(0xFF008080)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF008080),
                            focusedLabelColor = Color(0xFF008080)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val safeAppName = customAppName.trim()
                            if (safeAppName.isBlank()) {
                                errorMessage = "Enter the app name first."
                                return@Button
                            }

                            val safePackageName = customPackageName.trim().ifBlank {
                                safeAppName.lowercase().replace("\\s+".toRegex(), ".")
                            }

                            scope.launch {
                                sendRequest(
                                    appName = safeAppName,
                                    packageName = safePackageName,
                                    reason = customReason.trim().ifBlank { "Please allow access to $safeAppName." }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = !isSending,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF008080))
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Send Custom Request", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onBack,
                enabled = !isSending,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Back to Home", color = Color.Gray)
            }
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LaunchedEffect(showSuccess) {
                if (showSuccess) {
                    delay(1800)
                    onSent()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Color.White,
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color(0xFF00BFA5)
                        ) {
                            androidx.compose.material.Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Request Sent!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your parent has been notified and can review it now.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildSummaryCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
            Text(title, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyAppsCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 2.dp
    ) {
        Text(
            text = message,
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AppAccessCard(
    app: ManagedAppDto,
    isAllowed: Boolean,
    isRequested: Boolean,
    isSending: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color.White,
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isAllowed) Color(0xFF00BFA5).copy(alpha = 0.12f) else Color(0xFFD32F2F).copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material.Icon(
                    imageVector = if (isAllowed) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isAllowed) Color(0xFF00BFA5) else Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.app_name, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                Text(
                    app.category ?: app.package_name,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (isAllowed) {
                OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(50)) {
                    androidx.compose.material.Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF00BFA5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Allowed", color = Color(0xFF00BFA5))
                }
            } else {
                Button(
                    onClick = onRequest,
                    enabled = !isSending && !isRequested,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF008080))
                ) {
                    if (isSending && !isRequested) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (isRequested) "Request Sent" else "Request Access",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
