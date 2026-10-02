package com.example.kioskyapp.screentime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.kioskyapp.R
import com.example.kioskyapp.apiServices.ScreenTimeApi
import com.example.kioskyapp.data.Result
import com.example.kioskyapp.utils.KioskySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Tracks screen-on time and shows the kioskyApp lock screen when the daily limit is exceeded.
 *
 * The custom lock screen keeps SOS available while the device is paused.
 */
class ScreenTimeLockService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var loopJob: Job? = null
    private var lastSyncedMinutes: Int? = null

    private lateinit var settings: KioskySettings
    private val api = ScreenTimeApi()

    private var isScreenOn = true
    private var lastOnElapsedMs: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    lastOnElapsedMs = SystemClock.elapsedRealtime()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                }
                Intent.ACTION_USER_PRESENT -> {
                    scope.launch { checkAndLockIfNeeded(force = true) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = KioskySettings(applicationContext)

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        )

        lastOnElapsedMs = SystemClock.elapsedRealtime()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, buildNotification("Screen time tracking active"), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, buildNotification("Screen time tracking active"))
        }

        loopJob = scope.launch {
            while (isActive) {
                checkAndLockIfNeeded(force = false)
                delay(5_000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loopJob?.cancel()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun todayKey(childId: String, deviceId: String): String =
        "screen_time_used_ms_${childId}_${deviceId}_${LocalDate.now()}"

    private fun activeRuleCreatedAtKey(childId: String, deviceId: String): String =
        "screen_time_rule_created_at_${childId}_${deviceId}"

    private fun lockWindowEndKey(childId: String, deviceId: String): String =
        "screen_time_lock_until_${childId}_${deviceId}"

    private fun setLiveUsedMs(value: Long) {
        settings.putString("screen_time_used_live_ms", value.toString())
    }

    private fun getUsedMsToday(childId: String, deviceId: String): Long =
        settings.getString(todayKey(childId, deviceId), "0")?.toLongOrNull() ?: 0L

    private fun setUsedMsToday(childId: String, deviceId: String, value: Long) {
        settings.putString(todayKey(childId, deviceId), value.toString())
        setLiveUsedMs(value)
    }

    private fun getLockWindowEndMs(childId: String, deviceId: String): Long =
        settings.getString(lockWindowEndKey(childId, deviceId), "0")?.toLongOrNull() ?: 0L

    private fun setLockWindowEndMs(childId: String, deviceId: String, value: Long) {
        settings.putString(lockWindowEndKey(childId, deviceId), value.toString())
        settings.putString(LOCK_SCREEN_UNLOCK_AT_KEY, value.toString())
    }

    private fun clearLockWindow(childId: String, deviceId: String) {
        settings.remove(lockWindowEndKey(childId, deviceId))
        settings.remove(LOCK_SCREEN_UNLOCK_AT_KEY)
    }

    private fun addScreenOnDeltaToToday(childId: String, deviceId: String): Long {
        if (!isScreenOn) return getUsedMsToday(childId, deviceId)
        val now = SystemClock.elapsedRealtime()
        val delta = (now - lastOnElapsedMs).coerceAtLeast(0L)
        lastOnElapsedMs = now
        val updated = getUsedMsToday(childId, deviceId) + delta
        setUsedMsToday(childId, deviceId, updated)
        return updated
    }

    private fun maybeResetUsageForRule(childId: String, deviceId: String, ruleCreatedAt: String) {
        val storedRuleCreatedAt = settings.getString(activeRuleCreatedAtKey(childId, deviceId))
        if (storedRuleCreatedAt == ruleCreatedAt) return

        setUsedMsToday(childId, deviceId, 0L)
        settings.putString(activeRuleCreatedAtKey(childId, deviceId), ruleCreatedAt)
        clearLockWindow(childId, deviceId)
        setLockScreenActive(false)
        lastOnElapsedMs = SystemClock.elapsedRealtime()
        lastSyncedMinutes = null
    }

    private fun finishTimedLock(childId: String, deviceId: String) {
        clearLockWindow(childId, deviceId)
        setUsedMsToday(childId, deviceId, 0L)
        setLockScreenActive(false)
        dismissKioskyLockScreen()
        lastOnElapsedMs = SystemClock.elapsedRealtime()
        lastSyncedMinutes = null
    }

    private suspend fun checkAndLockIfNeeded(force: Boolean) {
        val token = settings.getString("child_token")
        val childId = settings.getString("child_id")
        val deviceId = settings.getString("device_id")

        if (token.isNullOrBlank() || childId.isNullOrBlank() || deviceId.isNullOrBlank()) return

        val limitRes = api.getActiveRuleForChild(token, childId)
        val activeRule = if (limitRes is Result.Success && limitRes.data.isNotEmpty()) {
            limitRes.data.first()
        } else null
        val limitMinutes = activeRule?.daily_limit_min ?: 0
        val unlockAfterMinutes = activeRule?.unlock_after_min ?: 30

        if (activeRule != null) {
            maybeResetUsageForRule(childId, deviceId, activeRule.created_at)
        } else {
            clearLockWindow(childId, deviceId)
        }

        val nowWallClockMs = System.currentTimeMillis()
        val lockUntilMs = getLockWindowEndMs(childId, deviceId)
        if (lockUntilMs > 0L) {
            if (nowWallClockMs >= lockUntilMs) {
                finishTimedLock(childId, deviceId)
            } else {
                lastOnElapsedMs = SystemClock.elapsedRealtime()
                showKioskyLockScreen(force = !ScreenTimeLockActivity.isWhitelistedAccessActive(settings))
                return
            }
        }

        val usedMs = addScreenOnDeltaToToday(childId, deviceId)
        val usedMinutes = (usedMs / 60_000L).toInt()
        val limitMs = limitMinutes * 60_000L

        if (lastSyncedMinutes != usedMinutes) {
            api.upsertTodayUsageMinutes(
                token = token,
                childId = childId,
                deviceId = deviceId,
                usageDateIso = LocalDate.now().toString(),
                totalMinutes = usedMinutes
            )
            lastSyncedMinutes = usedMinutes
        }

        if (limitMinutes > 0 && usedMs >= limitMs) {
            if (unlockAfterMinutes > 0) {
                setLockWindowEndMs(childId, deviceId, nowWallClockMs + (unlockAfterMinutes * 60_000L))
            }
            showKioskyLockScreen(force = !ScreenTimeLockActivity.isWhitelistedAccessActive(settings))
        } else {
            dismissKioskyLockScreen()
            if (force) {
                // keep the explicit force path for unlock checks
            }
        }
    }

    private fun showKioskyLockScreen(force: Boolean = true) {
        if (!force) return
        setLockScreenActive(true)
        startActivity(ScreenTimeLockActivity.createLaunchIntent(this))
    }

    private fun dismissKioskyLockScreen() {
        ScreenTimeLockActivity.clearWhitelistedAppAccess(this)
        if (settings.getString(LOCK_SCREEN_ACTIVE_KEY, "false") != "true") return
        setLockScreenActive(false)
        sendBroadcast(Intent(ScreenTimeLockActivity.ACTION_DISMISS_LOCK_SCREEN).setPackage(packageName))
    }

    private fun setLockScreenActive(active: Boolean) {
        settings.putString(LOCK_SCREEN_ACTIVE_KEY, active.toString())
    }

    private fun buildNotification(text: String): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "kioskyApp Screen Time",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("kioskyApp")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "kioskyapp_screen_time"
        private const val NOTIF_ID = 9901
        const val LOCK_SCREEN_ACTIVE_KEY = "screen_time_lock_active"
        const val LOCK_SCREEN_UNLOCK_AT_KEY = "screen_time_lock_unlock_at_ms"

        fun start(context: Context) {
            val intent = Intent(context, ScreenTimeLockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
