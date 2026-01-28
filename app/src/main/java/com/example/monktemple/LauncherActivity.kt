package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.monktemple.Utlis.SessionManager
import com.google.firebase.auth.FirebaseAuth

class LauncherActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_launcher)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        checkUserStateAndNavigate()
    }

    private fun checkUserStateAndNavigate() {
        val currentUser = auth.currentUser

        Log.d("LauncherActivity", "=== APP START ===")
        Log.d("LauncherActivity", "isFirstTime: ${sessionManager.isFirstTime}")
        Log.d("LauncherActivity", "isTrulyFirstTime: ${sessionManager.isTrulyFirstTime()}")
        Log.d("LauncherActivity", "isLoggedIn: ${sessionManager.isLoggedIn}")
        Log.d("LauncherActivity", "Firebase User: ${currentUser?.uid}")
        Log.d("LauncherActivity", "Firebase UID in prefs: ${sessionManager.getFirebaseUid()}")

        sessionManager.debugPrintAllData()

        when {
            // TRUE FIRST-TIME USER: No user data exists at all
            sessionManager.isTrulyFirstTime() -> {
                Log.d("LauncherActivity", "🆕 TRUE FIRST-TIME USER - Going to SignUp")
                startActivity(Intent(this, signUpActivity::class.java))
                finish()
            }

            // LOGGED-IN USER: Session says logged in AND Firebase user exists
            sessionManager.isLoggedIn && currentUser != null -> {
                Log.d("LauncherActivity", "✅ LOGGED-IN USER - UID: ${currentUser.uid}")

                val shouldShowAuth = sessionManager.isAnyAuthEnabled()
                Log.d("LauncherActivity", "Auth required: $shouldShowAuth")

                if (shouldShowAuth) {
                    Log.d("LauncherActivity", "🔐 Navigating to AuthenticationActivity")
                    startActivity(Intent(this, AuthenticationActivity::class.java))
                } else {
                    Log.d("LauncherActivity", "⏰ Navigating directly to Timer")
                    startActivity(Intent(this, Timer::class.java))
                }
                finish()
            }

            // SIGNED-OUT RETURNING USER: Has user data but not currently logged in
            !sessionManager.isLoggedIn && sessionManager.getFirebaseUid() != null -> {
                Log.d("LauncherActivity", "🔁 RETURNING USER (signed out) - Going to Login")
                startActivity(Intent(this, Loginscreen::class.java))
                finish()
            }

            // DEFAULT: Go to login (shouldn't normally reach here)
            else -> {
                Log.d("LauncherActivity", "❓ DEFAULT CASE - Going to Login")
                startActivity(Intent(this, Loginscreen::class.java))
                finish()
            }
        }
    }
}