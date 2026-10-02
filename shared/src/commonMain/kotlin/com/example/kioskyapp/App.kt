package com.example.kioskyapp

import androidx.compose.runtime.*
import com.example.kioskyapp.ui.LoginScreen
import com.example.kioskyapp.ui.RegisterScreen
import com.example.kioskyapp.ui.RoleSelectionScreen
import com.example.kioskyapp.ui.SplashScreen
import com.example.kioskyapp.ui.OnboardingScreen
import com.example.kioskyapp.ui.parent.ParentDashboardScreen
import com.example.kioskyapp.ui.kid.KidDashboardScreen
import com.example.kioskyapp.ui.parent.ParentOnboardingScreen
import com.example.kioskyapp.models.ParentUser

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf("splash") }
    var userToken by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<ParentUser?>(null) }
    var loginSuccessMessage by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        "splash" -> SplashScreen(
            onTimeout = { currentScreen = "onboarding" }
        )
        "onboarding" -> OnboardingScreen(
            onGetStarted = { currentScreen = "role_selection" }
        )
        "role_selection" -> {
            BackHandler { currentScreen = "onboarding" }
            RoleSelectionScreen(
                onSelectChild = { currentScreen = "kid_dashboard" },
                onSelectParent = { currentScreen = "parent_onboarding" }
            )
        }
        "parent_onboarding" -> {
            BackHandler { currentScreen = "role_selection" }
            ParentOnboardingScreen(
                onFinish = { currentScreen = "register" },
                onSkip = { currentScreen = "login" }
            )
        }
        "register" -> {
            BackHandler { currentScreen = "parent_onboarding" }
            RegisterScreen(
                onRegisterSuccess = { _ -> 
                    loginSuccessMessage = "Registration successful! Please login."
                    currentScreen = "login" 
                },
                onNavigateToLogin = { 
                    loginSuccessMessage = null
                    currentScreen = "login" 
                }
            )
        }
        "login" -> {
            BackHandler { currentScreen = "role_selection" }
            LoginScreen(
                onLoginSuccess = { token, user ->
                    userToken = token
                    currentUser = user
                    loginSuccessMessage = null
                    currentScreen = "parent_dashboard" 
                },
                onNavigateToRegister = { 
                    loginSuccessMessage = null
                    currentScreen = "register" 
                },
                registrationSuccessMessage = loginSuccessMessage
            )
        }
        "parent_dashboard" -> {
            currentUser?.let { user ->
                ParentDashboardScreen(
                    onLogout = { 
                        userToken = ""
                        currentUser = null
                        currentScreen = "login" 
                    },
                    token = userToken,
                    parentUser = user
                )
            } ?: run {
                currentScreen = "login"
            }
        }
        "kid_dashboard" -> {
            BackHandler { currentScreen = "role_selection" }
            KidDashboardScreen(
                onLogout = { currentScreen = "role_selection" }
            )
        }
    }
}
