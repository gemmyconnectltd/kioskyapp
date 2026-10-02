package com.example.kioskyapp.screentime

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kioskyapp.ui.kid.KioskySOSButton
import com.example.kioskyapp.utils.KioskySettings
import kotlinx.coroutines.delay

class ScreenTimeLockActivity : ComponentActivity() {

    private lateinit var settings: KioskySettings
    private var dismissReceiverRegistered = false

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DISMISS_LOCK_SCREEN) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = KioskySettings(applicationContext)
        configureWindowForLockScreen()

        setContent {
            MaterialTheme {
                BackHandler(enabled = true) { }
                ScreenTimeLockScreen(settings = settings)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerDismissReceiver()
    }

    override fun onResume() {
        super.onResume()
        if (!isLockActive()) {
            finishAndRemoveTask()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isLockActive() && !isWhitelistedAccessActive()) {
            startActivity(createLaunchIntent(this))
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations && isLockActive() && !isWhitelistedAccessActive()) {
            startActivity(createLaunchIntent(this))
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (dismissReceiverRegistered) {
            unregisterReceiver(dismissReceiver)
            dismissReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun configureWindowForLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun registerDismissReceiver() {
        if (dismissReceiverRegistered) return
        val filter = IntentFilter(ACTION_DISMISS_LOCK_SCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(dismissReceiver, filter)
        }
        dismissReceiverRegistered = true
    }

    private fun isLockActive(): Boolean {
        return settings.getString(ScreenTimeLockService.LOCK_SCREEN_ACTIVE_KEY, "false") == "true"
    }

    private fun isWhitelistedAccessActive(): Boolean {
        return isWhitelistedAccessActive(settings)
    }

    companion object {
        const val ACTION_DISMISS_LOCK_SCREEN = "com.example.kioskyapp.action.DISMISS_LOCK_SCREEN"
        private const val ALLOWED_APP_ACCESS_UNTIL_KEY = "screen_time_lock_allowed_app_access_until_ms"
        private const val ALLOWED_APP_ACCESS_DURATION_MS = 10 * 60 * 1000L

        fun createLaunchIntent(context: Context): Intent {
            return Intent(context, ScreenTimeLockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
        }

        fun allowWhitelistedAppAccess(context: Context, durationMs: Long = ALLOWED_APP_ACCESS_DURATION_MS) {
            val settings = KioskySettings(context.applicationContext)
            settings.putString(
                ALLOWED_APP_ACCESS_UNTIL_KEY,
                (System.currentTimeMillis() + durationMs).toString()
            )
        }

        fun isWhitelistedAccessActive(settings: KioskySettings): Boolean {
            val allowedUntil = settings.getString(ALLOWED_APP_ACCESS_UNTIL_KEY, "0")?.toLongOrNull() ?: 0L
            return allowedUntil > System.currentTimeMillis()
        }

        fun clearWhitelistedAppAccess(context: Context) {
            KioskySettings(context.applicationContext).remove(ALLOWED_APP_ACCESS_UNTIL_KEY)
        }
    }
}

@Composable
private fun ScreenTimeLockScreen(settings: KioskySettings) {
    val context = LocalContext.current
    val childToken = remember { settings.getString("child_token") }
    val childId = remember { settings.getString("child_id") }
    val deviceId = remember { settings.getString("device_id") }
    val parentId = remember { settings.getString("parent_id") }
    val childName = remember { settings.getString("child_name") ?: "Your child" }
    var remainingMs by remember { mutableStateOf(readRemainingLockMs(settings)) }

    LaunchedEffect(Unit) {
        while (true) {
            remainingMs = readRemainingLockMs(settings)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF09111F))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Screen Time Is Up",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (remainingMs > 0L) {
                    "This phone will unlock automatically in ${formatDuration(remainingMs)}."
                } else {
                    "This phone is paused until a parent resets or extends the time."
                },
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
            if (remainingMs > 0L) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = formatCountdown(remainingMs),
                    color = Color(0xFF6FFFE9),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "If there is an emergency, use the SOS button below to alert your parent immediately.",
                color = Color(0xFFB8C5D6),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.88f)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Approved access",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Phone calls and Contacts stay available while the device is locked.",
                color = Color(0xFFB8C5D6),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LockScreenQuickActionButton(
                    label = "Phone",
                    onClick = {
                        launchWhitelistedApp(
                            context = context,
                            intent = Intent(Intent.ACTION_DIAL).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                            failureMessage = "Phone app is not available on this device."
                        )
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                LockScreenQuickActionButton(
                    label = "Contacts",
                    onClick = {
                        launchWhitelistedApp(
                            context = context,
                            intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                            failureMessage = "Contacts app is not available on this device."
                        )
                    }
                )
            }
        }

        KioskySOSButton(
            childToken = childToken,
            childId = childId,
            deviceUuid = deviceId,
            parentId = parentId,
            childName = childName
        )
    }
}

@Composable
private fun LockScreenQuickActionButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF3A506B),
            contentColor = Color.White
        )
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

private fun launchWhitelistedApp(
    context: Context,
    intent: Intent,
    failureMessage: String
) {
    val resolved = intent.resolveActivity(context.packageManager)
    if (resolved == null) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        return
    }

    ScreenTimeLockActivity.allowWhitelistedAppAccess(context)
    try {
        context.startActivity(intent)
        (context as? Activity)?.moveTaskToBack(true)
    } catch (_: Exception) {
        ScreenTimeLockActivity.clearWhitelistedAppAccess(context)
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun readRemainingLockMs(settings: KioskySettings): Long {
    val unlockAt = settings.getString(ScreenTimeLockService.LOCK_SCREEN_UNLOCK_AT_KEY, "0")?.toLongOrNull() ?: 0L
    if (unlockAt <= 0L) return 0L
    return (unlockAt - System.currentTimeMillis()).coerceAtLeast(0L)
}

private fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    } else {
        String.format("%02dm %02ds", minutes, seconds)
    }
}

private fun formatDuration(remainingMs: Long): String {
    val totalMinutes = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}
