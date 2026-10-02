package com.example.kioskyapp.ui.kid

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.NotificationApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.utils.rememberDeviceInfoProvider
import com.example.kioskyapp.utils.rememberKioskyLocationProvider
import kotlinx.coroutines.launch

@Composable
fun KioskySOSButton(
    childToken: String?,
    childId: String?,
    deviceUuid: String?,
    parentId: String?,
    childName: String = "Your child"
) {
    var showOverlay by remember { mutableStateOf(false) }
    var isSent by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val notificationApi = remember { NotificationApi() }
    val locationProvider = rememberKioskyLocationProvider()
    val deviceInfoProvider = rememberDeviceInfoProvider()

    // Floating Button Animation
    val infiniteTransition = rememberInfiniteTransition(label = "SOS button transition")
    val scale: Float by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SOS button scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // HIDE the Floating Button if overlay is showing OR if SOS is already sent (Help is on the way)
        if (!showOverlay && !isSent) {
            Button(
                onClick = { showOverlay = true },
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(64.dp)
                    .scale(scale),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5)),
                elevation = ButtonDefaults.elevation(defaultElevation = 8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // The Full-Screen SOS Overlay
        if (showOverlay || isSent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .pointerInput(Unit) {
                        detectTapGestures { /* Prevent clicks from passing through */ }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Close button only shows when NOT sending and NOT sent (initial state)
                if (!isSending && !isSent) {
                    IconButton(
                        onClick = { 
                            showOverlay = false
                            errorMessage = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                if (!isSent) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "EMERGENCY HELP",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap the button below to alert your parents immediately.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = Color.Red, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(64.dp))

                        Surface(
                            shape = CircleShape,
                            color = if (isSending) Color.Gray else Color.Red,
                            elevation = 8.dp,
                            modifier = Modifier
                                .size(180.dp)
                                .clickable(enabled = !isSending && childToken != null && childId != null && parentId != null) {
                                    isSending = true
                                    errorMessage = null
                                    scope.launch {
                                        try {
                                            val deviceInfo = deviceInfoProvider.getDeviceInfo("")
                                            val activeDeviceId = deviceUuid ?: deviceInfo.device_id
                                            val location = locationProvider.getCurrentLocation(childId!!, activeDeviceId)

                                            val result = notificationApi.sendSOS(
                                                token = childToken!!,
                                                parentId = parentId!!,
                                                childId = childId!!,
                                                childName = childName,
                                                latitude = location?.latitude,
                                                longitude = location?.longitude,
                                                deviceId = activeDeviceId
                                            )

                                            isSending = false
                                            if (result is Result.Success) {
                                                isSent = true
                                                showOverlay = false // Transition to "Sent" state view
                                            } else {
                                                errorMessage = "Failed to send alert. Try again."
                                            }
                                        } catch (e: Exception) {
                                            isSending = false
                                            errorMessage = "Error: ${e.message}"
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSending) {
                                    CircularProgressIndicator(color = Color.White)
                                } else {
                                    Text(
                                        text = "FOR EMERGENCY\nUSE ONLY",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }

                        if (childToken == null || childId == null || parentId == null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Authentication required to send SOS",
                                color = Color.Yellow,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Success/Sent State (Help is on the way)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(120.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Alert Sent",
                                tint = Color.White,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Help is on the way!",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your parents have been notified. Stay exactly where you are.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(64.dp))
                        
                        Button(
                            onClick = {
                                isSent = false
                                showOverlay = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("I am safe now", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
