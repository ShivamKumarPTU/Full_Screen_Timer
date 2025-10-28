package com.example.monktemple

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class
NotificationActivity : AppCompatActivity() {
    val   CHANNEL_ID = "my_channel_id"
    val   CHANNEL_NAME = "my_channel_name"
    val notificationId=1
 //   @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // creating the notification using Notification Compat
        val notificationBuilder= NotificationCompat.Builder(this,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("My Notification")
            .setContentText("This is my notification content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // initialising the notification Manager channel
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(notificationId,notificationBuilder)
    }
    fun createNotificationChannel(){
        val channel= NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description="This is my notification channel"
        }
        val manager=getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager
        manager.createNotificationChannel(channel)
    }
}