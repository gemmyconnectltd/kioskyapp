package com.example.kioskyapp.screentime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.kioskyapp.utils.KioskySettings

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = KioskySettings(context.applicationContext)
        val childToken = settings.getString("child_token")
        val childId = settings.getString("child_id")
        if (!childToken.isNullOrBlank() && !childId.isNullOrBlank()) {
            ScreenTimeLockService.start(context.applicationContext)
        }
    }
}

