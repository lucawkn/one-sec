package com.luca.appinterceptor.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.luca.appinterceptor.R
import com.luca.appinterceptor.ui.MainActivity

object Notifications {

    private const val CHANNEL_ID = "service_status"
    private const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Service-Status",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warnt, wenn der Interceptor-Dienst nicht mehr läuft"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun notifyServiceDown(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("App Interceptor ist inaktiv")
            .setContentText("Der Bedienungshilfen-Dienst läuft nicht. Tippen, um ihn wieder zu aktivieren.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Benachrichtigungen nicht erlaubt – ignorieren
        }
    }
}
