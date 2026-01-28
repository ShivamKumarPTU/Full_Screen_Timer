package com.example.monktemple

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.monktemple.RoomUser.UserViewModel
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import com.example.monktemple.databinding.ActivityAuthenticationBinding
import com.google.firebase.auth.FirebaseAuth

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fingerprintHelper: FingerprintHelper
    private lateinit var faceUnlockHelper: FaceHelper

    private val mUserViewModel: UserViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[UserViewModel::class.java]
    }

    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val verificationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                onAuthSuccess()
            } else {
                ShowToast("Authentication Cancelled")
                // Don't finish, let user try again
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        fingerprintHelper = FingerprintHelper(this)
        faceUnlockHelper = FaceHelper(this)

        setupAuthenticationMethods()
    }

    override fun onResume() {
        super.onResume()
        // Re-check authentication methods when returning from other activities
        setupAuthenticationMethods()
    }

    private fun setupAuthenticationMethods() {
        var isAnyAuthVisible = false

        // Reset all auth method visibility first
        binding.faceAuth.visibility = View.GONE
        binding.biometricAuth.visibility = View.GONE
        binding.passwordAuth.visibility = View.GONE
        binding.pinAuth.visibility = View.GONE

        Log.d("AuthActivity", "Checking authentication methods:")
        Log.d("AuthActivity", "Biometric: ${sessionManager.isBiometricEnabled}")
        Log.d("AuthActivity", "Password: ${sessionManager.isPasswordEnabled}")
        Log.d("AuthActivity", "PIN: ${sessionManager.isPinEnabled}")

        // Fingerprint
        if (sessionManager.isBiometricEnabled && fingerprintHelper.isFingerprintAvailable()) {
            binding.biometricAuth.visibility = View.VISIBLE
            binding.biometricAuth.setOnClickListener { authenticateWithBiometric() }
            isAnyAuthVisible = true
            Log.d("AuthActivity", "✅ Biometric enabled and available")
        }

        // Password
        if (sessionManager.isPasswordEnabled) {
            binding.passwordAuth.visibility = View.VISIBLE
            binding.passwordAuth.setOnClickListener { authenticateWithPassword() }
            isAnyAuthVisible = true
            Log.d("AuthActivity", "✅ Password enabled")
        }

        // PIN
        if (sessionManager.isPinEnabled) {
            binding.pinAuth.visibility = View.VISIBLE
            binding.pinAuth.setOnClickListener { authenticateWithPin() }
            isAnyAuthVisible = true
            Log.d("AuthActivity", "✅ PIN enabled")
        }

        Log.d("AuthActivity", "Any auth visible: $isAnyAuthVisible")

        // Safeguard: If no auth methods are enabled/available, proceed directly
        if (!isAnyAuthVisible) {
            Log.d("AuthActivity", "❌ No auth methods available, proceeding to main app")
            proceedToMainApp()
        }
    }
    private fun authenticateWithBiometric() {

        fingerprintHelper.showFingerprintDialog(
            this,
            onSuccess = { onAuthSuccess() },
            onError = { error ->
                ShowToast("Fingerprint error: $error")
                // Don't finish, let user try other methods
            },
            onFailed = {
                ShowToast("Authentication failed")
                // Don't finish, let user try again
            }
        )
    }


    private fun authenticateWithPassword() {
        val intent = Intent(this, PasswordVerificationActivity::class.java)
        verificationLauncher.launch(intent)
    }

    private fun authenticateWithPin() {
        val intent = Intent(this, PinVerificationActivity::class.java)
        verificationLauncher.launch(intent)
    }

    private fun onAuthSuccess() {
        ShowToast("Authentication Successful")
        proceedToMainApp()
    }

    private fun proceedToMainApp() {
        val intent = Intent(this, Timer::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Prevent going back to login screen
        finishAffinity()
    }

    override fun onStop() {
        super.onStop()

    }
}