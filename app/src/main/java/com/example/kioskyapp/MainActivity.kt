package com.example.kioskyapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.kioskyapp.apiServices.configureApiBaseUrl
import com.example.kioskyapp.navigation.AppNavigationBridge
import com.example.kioskyapp.screentime.ScreenTimeLockService
import com.example.kioskyapp.screentime.KioskyDeviceAdminReceiver
import com.example.kioskyapp.vpn.KioskyAccessibilityService
import com.example.kioskyapp.utils.KioskySettings

class MainActivity : ComponentActivity() {

    companion object {
        private const val SYSTEM_PROMPT_COOLDOWN_MS = 1800L
        private const val VPN_RUNNING_PREF_KEY = "vpn_service_running"
        private const val VPN_STATUS_MESSAGE_PREF_KEY = "vpn_status_message"
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("kiosky_prefs", Context.MODE_PRIVATE)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRequestingDeviceAdmin = false
    private var isRequestingVpn = false
    private var isRequestingAccessibility = false
    private var isRequestingLocation = false
    private var isSetupStepDialogShowing = false
    private var isProtectionTipsDialogShowing = false
    private var isVpnStatusDialogShowing = false
    private var screenTimeProtectionStarted = false
    private var vpnProtectionStarted = false
    private var isActivityResumed = false
    private var lastSystemPromptReturnAt = 0L

    private val childProtectionRunnable = Runnable {
        maybeStartChildProtection()
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "child_token" || key == "child_id" || key == "device_id") {
            scheduleChildProtectionCheck()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val requestedForegroundLocation = permissions.containsKey(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
            permissions.containsKey(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasFineLocation =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (requestedForegroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (hasFineLocation || hasCoarseLocation) &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
            return@registerForActivityResult
        }

        isRequestingLocation = false
        lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
        scheduleChildProtectionCheck()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    private val deviceAdminResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isRequestingDeviceAdmin = false
        lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
        scheduleChildProtectionCheck()
    }

    private val vpnResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isRequestingVpn = false
        lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
        if (result.resultCode == RESULT_OK || android.net.VpnService.prepare(this) == null) {
            scheduleChildProtectionCheck(SYSTEM_PROMPT_COOLDOWN_MS + 400L)
        } else {
            scheduleChildProtectionCheck()
        }
    }

    private val accessibilityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isRequestingAccessibility = false
        lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
        scheduleChildProtectionCheck()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleNavigationIntent(intent)

        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        configureApiBaseUrl(BuildConfig.API_BASE_URL)

        // Android 13+ needs notification permission for foreground service notification
        requestPostNotificationsPermissionIfNeeded()

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        vpnProtectionStarted = isVpnServiceRunning()
        scheduleChildProtectionCheck()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(childProtectionRunnable)
        isActivityResumed = false
        super.onPause()
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDestroy()
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val destination = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_PARENT_SCREEN)
        if (!destination.isNullOrBlank()) {
            AppNavigationBridge.openParentScreen(destination)
            intent.removeExtra(NotificationHelper.EXTRA_OPEN_PARENT_SCREEN)
        }
    }

    private fun scheduleChildProtectionCheck(delayMs: Long? = null) {
        mainHandler.removeCallbacks(childProtectionRunnable)

        if (!isActivityResumed) return

        val elapsedSincePrompt = SystemClock.elapsedRealtime() - lastSystemPromptReturnAt
        val safeDelay = delayMs ?: if (elapsedSincePrompt < SYSTEM_PROMPT_COOLDOWN_MS) {
            SYSTEM_PROMPT_COOLDOWN_MS - elapsedSincePrompt
        } else {
            250L
        }

        mainHandler.postDelayed(childProtectionRunnable, safeDelay.coerceAtLeast(0L))
    }

    private fun maybeStartChildProtection() {
        if (!isActivityResumed) return
        if (isRequestingDeviceAdmin || isRequestingVpn || isRequestingAccessibility || isRequestingLocation || isProtectionTipsDialogShowing || isSetupStepDialogShowing) return

        if (vpnProtectionStarted && !isVpnServiceRunning()) {
            vpnProtectionStarted = false
        }

        consumeVpnStatusMessage()?.let { message ->
            vpnProtectionStarted = false
            showVpnStatusMessage(message)
            return
        }

        if (!hasCompleteChildSession()) {
            isRequestingDeviceAdmin = false
            isRequestingVpn = false
            isRequestingAccessibility = false
            isRequestingLocation = false
            isSetupStepDialogShowing = false
            isProtectionTipsDialogShowing = false
            isVpnStatusDialogShowing = false
            screenTimeProtectionStarted = false
            vpnProtectionStarted = false
            return
        }

        continueChildProtectionSetup()
    }

    private fun continueChildProtectionSetup() {
        if (!isActivityResumed) return

        val settings = KioskySettings(applicationContext)
        val childToken = settings.getString("child_token")
        val childId = settings.getString("child_id")
        if (childToken.isNullOrBlank() || childId.isNullOrBlank()) return

        if (!isAccessibilityServiceEnabled()) {
            promptForProtectionStep(
                title = "Enable App Protection",
                message = "kioskyApp needs Accessibility access to block apps like Instagram, TikTok, and games when a parent disables them.",
                onContinue = { ensureAccessibilityEnabled() }
            )
            return
        }

        if (!hasLocationPermissions()) {
            promptForProtectionStep(
                title = "Enable Location Tracking",
                message = "kioskyApp needs location access to send live location and emergency updates to the parent.",
                onContinue = { ensureLocationPermissions() }
            )
            return
        }

        startScreenTimeProtection()

        val elapsedSincePrompt = SystemClock.elapsedRealtime() - lastSystemPromptReturnAt
        if (elapsedSincePrompt < SYSTEM_PROMPT_COOLDOWN_MS) {
            scheduleChildProtectionCheck(SYSTEM_PROMPT_COOLDOWN_MS - elapsedSincePrompt)
            return
        }

        if (!isVpnReady()) {
            promptForProtectionStep(
                title = "Enable Web Protection",
                message = "kioskyApp needs VPN permission to block websites and apply safe browsing rules on the child phone.",
                onContinue = { prepareVpn() }
            )
            return
        }

        startVpnProtection()
    }

    private fun maybeShowProtectionTips(onComplete: () -> Unit) {
        // One-time heads-up so parents know how to keep filtering always active.
        val shown = prefs.getBoolean("protection_tips_shown", false)
        if (shown) {
            onComplete()
            return
        }
        if (isProtectionTipsDialogShowing) return
        if (!isActivityResumed) return

        isProtectionTipsDialogShowing = true
        prefs.edit().putBoolean("protection_tips_shown", true).apply()

        android.app.AlertDialog.Builder(this)
            .setTitle("Keep Protection Always On")
            .setMessage(
                "For continuous web filtering:\n\n" +
                    "1) Settings > VPN > kioskyApp: enable Always-on VPN + Block without VPN\n" +
                    "2) Settings > Network & Internet > Private DNS: turn Off\n\n" +
                    "If these are not enabled, filtering may stop when VPN is off."
            )
            .setPositiveButton("Open VPN settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                isProtectionTipsDialogShowing = false
                lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
                onComplete()
            }
            .setNegativeButton("Later") { _, _ ->
                isProtectionTipsDialogShowing = false
                onComplete()
            }
            .setOnCancelListener {
                isProtectionTipsDialogShowing = false
                onComplete()
            }
            .show()
    }

    private fun promptForProtectionStep(
        title: String,
        message: String,
        onContinue: () -> Unit
    ) {
        if (isSetupStepDialogShowing) return
        if (!isActivityResumed) return

        isSetupStepDialogShowing = true

        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                isSetupStepDialogShowing = false
                onContinue()
            }
            .setNegativeButton("Later") { _, _ ->
                isSetupStepDialogShowing = false
            }
            .setOnCancelListener {
                isSetupStepDialogShowing = false
            }
            .show()
    }

    private fun ensureDeviceAdminEnabled() {
        if (isRequestingDeviceAdmin || isDeviceAdminEnabled()) return

        // Prompt user to enable Device Admin so we can lock the device at time limit
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(this@MainActivity, KioskyDeviceAdminReceiver::class.java)
            )
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "kioskyApp needs device admin permission to lock the phone when screen time limit is reached."
            )
        }
        isRequestingDeviceAdmin = true
        deviceAdminResultLauncher.launch(intent)
    }

    private fun ensureAccessibilityEnabled() {
        if (isRequestingAccessibility || isAccessibilityServiceEnabled()) return
        if (!isActivityResumed) return
        isRequestingAccessibility = true
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilityResultLauncher.launch(intent)
    }

    private fun ensureLocationPermissions() {
        if (isRequestingLocation || hasLocationPermissions()) return
        if (!isActivityResumed) return
        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            isRequestingLocation = true
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                isRequestingLocation = true
                requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            } else {
                isRequestingLocation = false
                lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
                scheduleChildProtectionCheck()
            }
        } else {
            isRequestingLocation = false
            lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
            scheduleChildProtectionCheck()
        }
    }

    private fun prepareVpn() {
        if (isRequestingVpn) return
        if (!isActivityResumed) return

        val intent = android.net.VpnService.prepare(this)
        if (intent != null) {
            isRequestingVpn = true
            vpnResultLauncher.launch(intent)
        } else {
            startVpnProtection()
        }
    }

    private fun startScreenTimeProtection() {
        if (screenTimeProtectionStarted) return

        screenTimeProtectionStarted = true
        ScreenTimeLockService.start(applicationContext)
    }

    private fun startVpnProtection() {
        if (isVpnServiceRunning()) {
            vpnProtectionStarted = true
            return
        }
        if (vpnProtectionStarted) return
        if (!isActivityResumed) {
            scheduleChildProtectionCheck(1000L)
            return
        }

        vpnProtectionStarted = true
        mainHandler.postDelayed({
            if (!isActivityResumed) {
                vpnProtectionStarted = false
                return@postDelayed
            }

            try {
                startVpnService()
            } catch (_: Exception) {
                vpnProtectionStarted = false
                lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
                scheduleChildProtectionCheck(1500L)
            }
        }, 1200L)
    }

    private fun hasCompleteChildSession(): Boolean {
        val settings = KioskySettings(applicationContext)
        val childToken = settings.getString("child_token")
        val childId = settings.getString("child_id")
        val deviceId = settings.getString("device_id")
        return !childToken.isNullOrBlank() && !childId.isNullOrBlank() && !deviceId.isNullOrBlank()
    }

    private fun isDeviceAdminEnabled(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, KioskyDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, KioskyAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        return TextUtils.SimpleStringSplitter(':').run {
            setString(enabledServices)
            any { it.equals(expectedComponentName, ignoreCase = true) }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val hasForeground =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasForeground) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun isVpnReady(): Boolean {
        return android.net.VpnService.prepare(this) == null
    }

    private fun startVpnService() {
        val intent = Intent(this, com.example.kioskyapp.vpn.KioskyVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun isVpnServiceRunning(): Boolean {
        return prefs.getBoolean(VPN_RUNNING_PREF_KEY, false)
    }

    private fun consumeVpnStatusMessage(): String? {
        val message = prefs.getString(VPN_STATUS_MESSAGE_PREF_KEY, null)?.trim().orEmpty()
        if (message.isBlank()) return null
        prefs.edit().remove(VPN_STATUS_MESSAGE_PREF_KEY).apply()
        return message
    }

    private fun showVpnStatusMessage(message: String) {
        if (!isActivityResumed) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }
        if (isVpnStatusDialogShowing) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }

        isVpnStatusDialogShowing = true
        android.app.AlertDialog.Builder(this)
            .setTitle("Web Protection Needs Attention")
            .setMessage(
                "$message\n\n" +
                    "If you use another VPN on this phone, turn it off before starting kioskyApp web protection."
            )
            .setPositiveButton("Open VPN Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                isVpnStatusDialogShowing = false
                lastSystemPromptReturnAt = SystemClock.elapsedRealtime()
                scheduleChildProtectionCheck(SYSTEM_PROMPT_COOLDOWN_MS + 400L)
            }
            .setNegativeButton("Later") { _, _ ->
                isVpnStatusDialogShowing = false
            }
            .setOnCancelListener {
                isVpnStatusDialogShowing = false
            }
            .show()
    }

}
