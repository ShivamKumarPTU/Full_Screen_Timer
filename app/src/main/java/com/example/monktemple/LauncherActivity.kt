package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.monktemple.Utlis.SessionManager
//import com.example.gymsaathi.utils.SessionManager
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
    private fun checkUserStateAndNavigate(){

        val currentUser = auth.currentUser

        Log.d("LauncherActivity", "=== APP START ===")
        Log.d("LauncherActivity", "isFirstTime: ${sessionManager.isFirstTime}")
        Log.d("LauncherActivity", "isLoggedIn: ${sessionManager.isLoggedIn}")
        Log.d("LauncherActivity", "isAuthRequired: ${sessionManager.isAuthRequired}")
        Log.d("LauncherActivity", "isAnyAuthEnabled: ${sessionManager.isAnyAuthEnabled()}")

        sessionManager.debugPrintAllData()

        when{
            sessionManager.isFirstTime -> {
                Log.d("LauncherActivity", "Navigating to SignUp (First time)")
                // DON'T call resetAllAuthentication here - let it be handled by the setup flow
                startActivity(Intent(this, signUpActivity::class.java))
                finish()
            }
            sessionManager.isLoggedIn && currentUser != null -> {
                Log.d("LauncherActivity", "User is logged in")

                // CRITICAL: Only check if any auth is enabled, don't check isAuthRequired separately
                val shouldShowAuth = sessionManager.isAnyAuthEnabled()

                Log.d("LauncherActivity", "Should show authentication: $shouldShowAuth")
                Log.d("LauncherActivity", "Auth methods enabled: ${sessionManager.isAnyAuthEnabled()}")

                if (shouldShowAuth) {
                    Log.d("LauncherActivity", "Navigating to AuthenticationActivity")
                    startActivity(Intent(this, AuthenticationActivity::class.java))
                } else {
                    Log.d("LauncherActivity", "Navigating directly to Timer")
                    startActivity(Intent(this, Timer::class.java))
                }
                finish()
            }
            else -> {
                Log.d("LauncherActivity", "Navigating to Login screen")
                // Only reset if truly needed
                if (!sessionManager.isLoggedIn) {
                    sessionManager.resetAllAuthentication()
                }
                startActivity(Intent(this, Loginscreen::class.java))
                finish()
            }
        }
    }
}