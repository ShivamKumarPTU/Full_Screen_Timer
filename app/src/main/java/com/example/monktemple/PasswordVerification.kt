package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.monktemple.Utlis.DialogManager
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.example.monktemple.data.sync.UserDataManager

class PasswordVerificationActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var passwordInput: EditText
    private lateinit var verifyButton: Button
    private lateinit var cancelButton: Button
    private lateinit var forgetPasswordButton: Button
    private lateinit var fingerprintHelper: FingerprintHelper

    private val PREF_PASSWORD = "user_password"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_password_verification)

        sessionManager = SessionManager(this)

        setupViews()
        setupClickListeners()
        forgetPasswordButton = findViewById(R.id.forgotPassword)
        forgetPasswordButton.setOnClickListener {
            forgotPassword()
        }
    }

    private fun setupViews() {
        passwordInput = findViewById(R.id.passwordInput)
        verifyButton = findViewById(R.id.verifyButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun setupClickListeners() {
        verifyButton.setOnClickListener {
            val enteredPassword = passwordInput.text.toString().trim()

            if (enteredPassword.isEmpty()) {
                ShowToast("Please enter your password")
                return@setOnClickListener
            }

            if (verifyPassword(enteredPassword)) {
                // Password correct
                setResult(RESULT_OK)
                finish()
            } else {
                ShowToast("Incorrect password")
                passwordInput.text.clear()
            }
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun verifyPassword(enteredPassword: String): Boolean {
        val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val storedPassword = sharedPref.getString(PREF_PASSWORD, "")
        return enteredPassword == storedPassword
    }

    private fun forgotPassword() {
        fingerprintHelper = FingerprintHelper(this)
        fingerprintHelper.showFingerprintDialog(
            this,
            onSuccess = {
                ShowToast("Fingerprint verified")
                resetPassword()
            },
            onError = { error ->
                ShowToast("Fingerprint error: $error")
            },
            onFailed = {
                ShowToast("Authentication failed")
            }
        )
    }

    private fun resetPassword() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.new_password_input)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.confirm_password_input)
        val resetButton = dialogView.findViewById<Button>(R.id.reset_password_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_password_button)

        val dialog = dialogBuilder.setView(dialogView).create()

        resetButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                ShowToast("Please enter your password")
                return@setOnClickListener
            }

            // Use the same validation function as setPasswordActivity
            val validationResult = validatePassword(newPassword)
            if (!validationResult.first) {
                ShowToast(validationResult.second)
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                ShowToast("Passwords do not match")
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            sharedPref.edit().putString(PREF_PASSWORD, newPassword).apply()
            ShowToast("Password reset successfully")
            dialog.dismiss()
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Same validation function as in setPasswordActivity
    private fun validatePassword(password: String): Pair<Boolean, String> {
        // Check minimum length
        if (password.length < 8) {
            return Pair(false, "Password must be at least 8 characters")
        }

        // Check for uppercase letter
        if (!password.any { it.isUpperCase() }) {
            return Pair(false, "Password must contain at least one uppercase letter")
        }

        // Check for number
        if (!password.any { it.isDigit() }) {
            return Pair(false, "Password must contain at least one number")
        }

        // Check for special character
        val specialChars = setOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=', '{', '}', '[', ']', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '?', '/')
        if (!password.any { it in specialChars }) {
            return Pair(false, "Password must contain at least one special character")
        }

        return Pair(true, "Valid password")
    }
}