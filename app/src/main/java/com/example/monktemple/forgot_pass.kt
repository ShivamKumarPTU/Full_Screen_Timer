package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class forgot_pass : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_pass)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize Firebase Auth
        auth = Firebase.auth
       sessionManager=SessionManager(this)
        val backIcon = findViewById<ImageButton>(R.id.backwardIcon)
        val emailText = findViewById<EditText>(R.id.enterYourEmail)
        val sendLink = findViewById<Button>(R.id.sendResetLink)

        // Handle "Send Reset Link" button click
        sendLink.setOnClickListener {
            val userEmail = emailText.text.toString().trim()
            val currentUserEmail=sessionManager.getUserEmail()
            when {
                userEmail.isEmpty() -> ShowToast("Please enter your email")
                !isValidEmail(userEmail) -> ShowToast("Invalid email format")
                currentUserEmail!=userEmail -> ShowToast("Email does not match")
                else -> sendPasswordResetLink(userEmail)
            }
        }

        // Handle back button click
        backIcon.setOnClickListener {
            navigateToLoginScreen()
        }
    }
    /**
     * Sends a password reset email to the provided email address.
     */
    private fun sendPasswordResetLink(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    ShowToast("Reset link sent to $email")
                    navigateToLoginScreen() // Return to login after success
                } else {
                    ShowToast("Failed to send reset link: ${task.
                    exception?.message}")
                }
            }
    }

    /**
     * Validates if the email format is correct.
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Navigates back to the login screen.
     */
    private fun navigateToLoginScreen() {
        startActivity(Intent(this, Loginscreen::class.java))
        finish() // Close this activity to prevent returning via back button
    }
}