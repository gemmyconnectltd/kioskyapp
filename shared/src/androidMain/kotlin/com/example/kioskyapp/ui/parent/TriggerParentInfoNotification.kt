package com.example.kioskyapp.ui.parent

import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import com.example.kioskyapp.NotificationHelper

@Composable
actual fun TriggerParentInfoNotification(id: String, title: String, message: String) {
    val context = LocalContext.current

    LaunchedEffect(id) {
        NotificationHelper.createNotificationChannels(context)

        val notification = NotificationCompat.Builder(context, NotificationHelper.PARENT_INFO_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(NotificationHelper.createAlertsPendingIntent(context))
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.hashCode(), notification)
    }
}
