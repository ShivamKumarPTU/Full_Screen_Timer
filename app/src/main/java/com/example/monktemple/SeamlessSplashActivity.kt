package com.example.monktemple

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.monktemple.Utlis.SessionManager

@SuppressLint("CustomSplashScreen")
@Suppress("DEPRECATION")
class SeamlessSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MonkTemple)
        super.onCreate(savedInstanceState)

        // Ultra-fast navigation (like YouTube/Amazon)
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            initializeApp()
        }, 500) // Only 500ms delay

    }

    private fun initializeApp() {
        val sessionManager = SessionManager(this)
        val isLoggedIn = sessionManager.isLoggedIn
        val firebaseUid = sessionManager.getFirebaseUid()

        val targetActivity = if (isLoggedIn && !firebaseUid.isNullOrEmpty()) {
            Timer::class.java
        } else {
            Loginscreen::class.java
        }

        val intent = Intent(this, targetActivity)
        startActivity(intent)
        finish()

        // NO animations for instant transition
        overridePendingTransition(0, 0)
    }

}

