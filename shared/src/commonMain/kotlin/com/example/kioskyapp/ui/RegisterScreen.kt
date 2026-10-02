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
import androidx.compose.material.icons.filled.Person
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
import com.example.kioskyapp.shared.generated.resources.regestericon
import com.example.kioskyapp.apiServices.AuthApi
import com.example.kioskyapp.data.Result
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(onRegisterSuccess: (String) -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
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

        // Professional Logo using regestericon.png
        Image(
            painter = painterResource(Res.drawable.regestericon),
            contentDescription = "Register Icon",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Join Us Today",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Sign up to get started",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = fullName,
            onValueChange = { 
                fullName = it
                errorMessage = null
            },
            placeholder = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray) },
            enabled = !isLoading,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF00BFA5),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "By signing up you've agreed to our terms & conditions and privacy",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val trimmedFullName = fullName.trim()
                val trimmedEmail = email.trim()
                val trimmedPassword = password.trim()

                if (trimmedFullName.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                
                isLoading = true
                scope.launch {
                    val result = authApi.register(trimmedFullName, trimmedEmail, trimmedPassword)
                    isLoading = false
                    when (result) {
                        is Result.Success -> {
                            onRegisterSuccess(result.data.token)
                        }
                        is Result.Error -> {
                            errorMessage = result.exception.message ?: "Registration failed"
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
                Text("Register", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f))
            Text("  OR  ", color = Color.Gray, fontSize = 12.sp)
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* Google Sign Up */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF1F5F9)),
            elevation = ButtonDefaults.elevation(0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "G",
                    color = Color(0xFF4285F4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign Up with Google", color = Color.DarkGray, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Have an account? ", color = Color.Gray)
            TextButton(onClick = onNavigateToLogin, contentPadding = PaddingValues(0.dp), enabled = !isLoading) {
                Text("Login", color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold)
            }
        }
    }
}
