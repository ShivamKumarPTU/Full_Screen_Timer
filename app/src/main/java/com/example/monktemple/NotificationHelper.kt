// NotificationHelper.kt
package com.example.monktemple

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "my_channel_id"
    private const val CHANNEL_NAME = "my_channel_name"

    @SuppressLint("MissingPermission")
    fun showTimerFinishedNotification(context: Context) {
        createNotificationChannel(context)

        // Pending Intent
        val intent = Intent(context, TimerScreen::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP  or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_notifications",true)
            putExtra("notification_time",System.currentTimeMillis())
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

 val detailedText= "Well done! You have completed your meditation session. Take a moment to appreciate this peace before returning to your day."
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timer Finished! ⏰")
            .setContentText("Your meditation session is complete")
            //Add this styleto show detialed text
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1, notificationBuilder)
    }

    private fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT

            ).apply {
                description = "Shows notifications when timers finish"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}