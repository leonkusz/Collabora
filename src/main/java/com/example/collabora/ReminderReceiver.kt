package com.example.collabora

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val taskTitle =
            intent.getStringExtra("taskTitle")
                ?: "Tugas"

        val channelId =
            "collabora_reminder_channel"

        // ==============================
        // CREATE CHANNEL
        // ==============================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Reminder Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager =
                context.getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }

        // ==============================
        // BUILD NOTIFICATION
        // ==============================

        val notification =
            NotificationCompat.Builder(
                context,
                channelId
            )

                .setSmallIcon(R.mipmap.ic_launcher)

                .setContentTitle(
                    "Deadline Segera Tiba! ⏰"
                )

                .setContentText(
                    "$taskTitle harus segera dikumpulkan (1 menit lagi)!"
                )

                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )

                .setAutoCancel(true)

                .build()

        // ==============================
        // SHOW NOTIFICATION
        // ==============================

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}