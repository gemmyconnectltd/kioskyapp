package com.example.kioskyapp.ui.kid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.apiServices.AuthApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.utils.rememberDeviceInfoProvider
import com.example.kioskyapp.utils.rememberKioskySettings
import kotlinx.coroutines.launch

@Composable
fun KidAuthenticationPage(onBack: () -> Unit, onSuccess: (String, String, String, String, String) -> Unit) {
    var pincode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isPinValid = pincode.length == 6

    val scope = rememberCoroutineScope()
    val authApi = remember { AuthApi() }
    val deviceInfoProvider = rememberDeviceInfoProvider()
    val settings = rememberKioskySettings()

    Scaffold(
        topBar = {
            if (!isLoading) {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    backgroundColor = Color(0xFFF5F5F5),
                    elevation = 0.dp
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Activate Device",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF008080)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the 6-digit PIN from your parent's app",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(36.dp))

            OutlinedTextField(
                value = pincode,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isLetterOrDigit() }) {
                        pincode = it
                        errorMessage = null
                    }
                },
                label = { Text("Activation PIN") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 24.sp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF008080),
                    focusedLabelColor = Color(0xFF008080)
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        val activationRequest = deviceInfoProvider.getDeviceInfo(pincode)
                        val result = authApi.activateChild(activationRequest)
                        isLoading = false
                        if (result is Result.Success) {
                            // Persist session
                            settings.putString("child_token", result.data.token)
                            settings.putString("child_id", result.data.child.id)
                            settings.putString("device_id", result.data.device.id) // DB UUID
                            settings.putString("parent_id", result.data.child.parent_id ?: "UNKNOWN_PARENT")
                            settings.putString("child_name", result.data.child.full_name)

                            onSuccess(
                                result.data.token, 
                                result.data.child.id,
                                result.data.child.parent_id ?: "UNKNOWN_PARENT",
                                result.data.child.full_name,
                                result.data.device.id
                            )
                        } else if (result is Result.Error) {
                            errorMessage = result.exception.message ?: "Activation failed. Please check your PIN."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isPinValid && !isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF008080))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = "PROCEED", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
