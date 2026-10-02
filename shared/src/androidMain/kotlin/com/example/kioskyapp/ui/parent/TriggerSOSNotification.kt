package com.example.kioskyapp.ui.parent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.kioskyapp.NotificationHelper

@Composable
actual fun TriggerSOSNotification(id: String, title: String, message: String) {
    val context = LocalContext.current
    LaunchedEffect(id) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = NotificationHelper.SOS_CHANNEL_ID
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: 
                      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check if channel exists, if not create it with high importance and alarm sound
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                channel = NotificationChannel(
                    channelId,
                    "Emergency SOS Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High priority emergency alerts"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                    setSound(soundUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(NotificationHelper.createAlertsPendingIntent(context))
            .setAutoCancel(true)
            .setFullScreenIntent(null, true) // High priority pop up
            .build()

        notificationManager.notify(id.hashCode(), notification)
    }
}
