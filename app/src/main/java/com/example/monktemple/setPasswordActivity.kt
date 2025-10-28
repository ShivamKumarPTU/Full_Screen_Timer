package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast

class setPasswordActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var setPasswordButton: Button

    private val PREF_PASSWORD = "user_password"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_password)

        sessionManager = SessionManager(this)

        setupViews()
        setupClickListeners()


    }

    private fun setupViews() {
        passwordInput = findViewById(R.id.newPasswordEditText)
        confirmPasswordInput = findViewById(R.id.confirmPasswordEditText)
        setPasswordButton = findViewById(R.id.savePasswordButton)
    }

    private fun setupClickListeners() {
        setPasswordButton.setOnClickListener {
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                ShowToast("Please fill all fields")
                return@setOnClickListener
            }

            // Validate password against all rules
            val validationResult = validatePassword(password)
            if (!validationResult.first) {
                ShowToast(validationResult.second)
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                ShowToast("Passwords do not match")
                return@setOnClickListener
            }

            // Store password securely (in real app, use encryption)
            val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(PREF_PASSWORD, password)
                apply()
            }

            sessionManager.isPasswordEnabled = true
            ShowToast("Password set successfully")
            finish()
        }
    }

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