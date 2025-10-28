package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast

class PinVerificationActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var pinInput: EditText
    private lateinit var verifyButton: Button
    private lateinit var cancelButton: Button
    private lateinit var forgotPasswordButton: Button
    private lateinit var fingerprintHelper: FingerprintHelper

    private val PREF_PIN = "user_pin"
    private var requiredPinLength = 4 // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pin_verification)

        sessionManager = SessionManager(this)

        // Get the stored pin length
        val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        requiredPinLength = sharedPref.getInt("pin_length", 4)

        setupViews()
        setupClickListeners()

        forgotPasswordButton.setOnClickListener {
            resetPin()
        }
    }

    private fun setupViews() {
        pinInput = findViewById(R.id.pinInput)
        verifyButton = findViewById(R.id.verifyButton)
        cancelButton = findViewById(R.id.cancelButton)
        forgotPasswordButton = findViewById(R.id.forgotPassword)

        // Set max length based on stored pin length
        pinInput.filters = arrayOf(android.text.InputFilter.LengthFilter(requiredPinLength))
        pinInput.hint = "$requiredPinLength digit PIN"
    }

    private fun setupClickListeners() {
        verifyButton.setOnClickListener {
            val enteredPin = pinInput.text.toString().trim()

            if (enteredPin.isEmpty() || enteredPin.length != requiredPinLength) {
                ShowToast("Please enter a valid $requiredPinLength-digit PIN")
                return@setOnClickListener
            }

            if (verifyPin(enteredPin)) {
                // PIN correct
                setResult(RESULT_OK)
                finish()
            } else {
                ShowToast("Incorrect PIN")
                pinInput.text.clear()
            }
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun verifyPin(enteredPin: String): Boolean {
        val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val storedPin = sharedPref.getString(PREF_PIN, "")
        return enteredPin == storedPin
    }

    private fun resetPin() {
        fingerprintHelper = FingerprintHelper(this)
        fingerprintHelper.showFingerprintDialog(
            this,
            onSuccess = {
                ShowToast("Fingerprint verified")
                pinChangeFunction()
            },
            onError = { error ->
                ShowToast("Fingerprint error: $error")
            },
            onFailed = {
                ShowToast("Authentication failed")
            }
        )
    }

    private fun pinChangeFunction() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_pin, null)
        dialogBuilder.setView(dialogView)
        val newPinEditText = dialogView.findViewById<EditText>(R.id.new_password_input)
        val confirmPinEditText = dialogView.findViewById<EditText>(R.id.confirm_password_input)
        val resetButton = dialogView.findViewById<Button>(R.id.reset_password_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_password_button)
        val dialog = dialogBuilder.setView(dialogView).create()
        // Set max length for reset dialog as well
        newPinEditText.filters = arrayOf(android.text.InputFilter.LengthFilter(requiredPinLength))
        confirmPinEditText.filters = arrayOf(android.text.InputFilter.LengthFilter(requiredPinLength))
        newPinEditText.hint = "Enter $requiredPinLength digit PIN"
        confirmPinEditText.hint = "Confirm $requiredPinLength digit PIN"

        resetButton.setOnClickListener {
            val newPin = newPinEditText.text.toString().trim()
            val confirmPin = confirmPinEditText.text.toString().trim()

            if (newPin.isEmpty() || confirmPin.isEmpty()) {
                ShowToast("Please enter your PIN")
                return@setOnClickListener
            }

            if (newPin.length != requiredPinLength) {
                ShowToast("PIN must be exactly $requiredPinLength digits")
                return@setOnClickListener
            }

            if (newPin != confirmPin) {
                ShowToast("PINs do not match")
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            sharedPref.edit().putString(PREF_PIN, newPin).apply()
            ShowToast("PIN reset successfully")
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
        }

        cancelButton.setOnClickListener {
            // Just close the dialog, don't finish the activity
            dialog.dismiss()
        }
        dialog.show()
    }
}