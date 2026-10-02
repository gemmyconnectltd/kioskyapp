package com.example.kioskyapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val SOS_CHANNEL_ID = "sos_emergency_channel"
    const val PARENT_INFO_CHANNEL_ID = "parent_info_channel"
    const val EXTRA_OPEN_PARENT_SCREEN = "open_parent_screen"
    const val SCREEN_ALERTS = "alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val name = "SOS Emergency Alerts"
            val descriptionText = "High priority alerts for child safety"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(SOS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500) // Repeated patterns for urgency
                
                // You can point to a raw resource for a custom alarm sound if you add one to res/raw
                // setSound(Uri.parse("android.resource://${context.packageName}/raw/emergency_alarm"), 
                //    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
            notificationManager.createNotificationChannel(channel)

            val parentInfoChannel = NotificationChannel(
                PARENT_INFO_CHANNEL_ID,
                "Parent Activity Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General parent updates such as app requests"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(parentInfoChannel)
        }
    }

    fun showSOSNotification(context: Context, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, SOS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setFullScreenIntent(null, true) // This can show a full screen activity like an incoming call

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun createAlertsPendingIntent(context: Context): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_PARENT_SCREEN, SCREEN_ALERTS)
            }
            ?: Intent().apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_OPEN_PARENT_SCREEN, SCREEN_ALERTS)
            }

        return PendingIntent.getActivity(
            context,
            SCREEN_ALERTS.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
