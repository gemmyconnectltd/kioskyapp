package com.example.kioskyapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import com.example.kioskyapp.shared.generated.resources.Res
import com.example.kioskyapp.shared.generated.resources.loginicon
import com.example.kioskyapp.apiServices.AuthApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.models.ParentUser
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String, ParentUser) -> Unit, 
    onNavigateToRegister: () -> Unit,
    registrationSuccessMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val authApi = remember { AuthApi() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Professional Logo using loginicon.png
        Image(
            painter = painterResource(Res.drawable.loginicon),
            contentDescription = "Login Icon",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hello Again!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Welcome back, You've been missed!",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (registrationSuccessMessage != null && errorMessage == null) {
            Text(
                text = registrationSuccessMessage,
                color = Color(0xFF00BFA5),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                errorMessage = null 
            },
            placeholder = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            trailingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.LightGray) },
            enabled = !isLoading,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF00BFA5),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null 
            },
            placeholder = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00BFA5)) },
            visualTransformation = PasswordVisualTransformation(),
            enabled = !isLoading,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF00BFA5),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { /* Forgot Password */ }, enabled = !isLoading) {
                Text("Forgot Password", color = Color(0xFF00BFA5), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val trimmedEmail = email.trim()
                val trimmedPassword = password.trim()

                if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                
                isLoading = true
                scope.launch {
                    val result = authApi.login(trimmedEmail, trimmedPassword)
                    isLoading = false
                    when (result) {
                        is Result.Success -> {
                            onLoginSuccess(result.data.token, result.data.user)
                        }
                        is Result.Error -> {
                            errorMessage = result.exception.message ?: "Login failed"
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Login", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f))
            Text("  OR  ", color = Color.Gray, fontSize = 12.sp)
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Professional Google Login Button
        Button(
            onClick = { /* Google Login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF1F5F9)),
            elevation = ButtonDefaults.elevation(0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Using a generic G representation for now as requested
                Text(
                    text = "G",
                    color = Color(0xFF4285F4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Login with Google", color = Color.DarkGray, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New Here? ", color = Color.Gray)
            TextButton(onClick = onNavigateToRegister, contentPadding = PaddingValues(0.dp), enabled = !isLoading) {
                Text("Register", color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold)
            }
        }
    }
}
