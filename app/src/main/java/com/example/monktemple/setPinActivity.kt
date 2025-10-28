package com.example.monktemple

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class setPinActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var pinInput: EditText
    private lateinit var confirmPinInput: EditText
    private lateinit var setPinButton: Button
    private lateinit var pinLengthToggleGroup: MaterialButtonToggleGroup
    private lateinit var button4Digits: MaterialButton
    private lateinit var button6Digits: MaterialButton

    private var selectedPinLength = 4 // Default to 4 digits
    private val PREF_PIN = "user_pin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_pin)

        sessionManager = SessionManager(this)

        setupViews()
        setupClickListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViews() {
        pinInput = findViewById(R.id.pinEditText)
        confirmPinInput = findViewById(R.id.confirmPinEditText)
        setPinButton = findViewById(R.id.confirmPinButton)
        pinLengthToggleGroup = findViewById(R.id.pinLengthToggleGroup)
        button4Digits = findViewById(R.id.button_4_digits)
        button6Digits = findViewById(R.id.button_6_digits)

        // Set default to 4 digits
        button4Digits.isChecked = true
        updatePinInputMaxLength()
    }

    private fun setupClickListeners() {
        pinLengthToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_4_digits -> selectedPinLength = 4
                    R.id.button_6_digits -> selectedPinLength = 6
                }
                updatePinInputMaxLength()
            }
        }

        setPinButton.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            val confirmPin = confirmPinInput.text.toString().trim()

            if (pin.isEmpty() || confirmPin.isEmpty()) {
                ShowToast("Please fill all fields")
                return@setOnClickListener
            }

            if (pin.length != selectedPinLength) {
                ShowToast("PIN must be exactly $selectedPinLength digits")
                return@setOnClickListener
            }

            if (!pin.matches(Regex("\\d+"))) {
                ShowToast("PIN must contain only numbers")
                return@setOnClickListener
            }

            if (pin != confirmPin) {
                ShowToast("PINs do not match")
                return@setOnClickListener
            }

            // Store PIN securely (in real app, use encryption)
            val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(PREF_PIN, pin)
                putInt("pin_length", selectedPinLength) // Store the pin length
                apply()
            }

            sessionManager.isPinEnabled = true
            ShowToast("PIN set successfully")
            finish()
        }
    }

    private fun updatePinInputMaxLength() {
        pinInput.filters = arrayOf(android.text.InputFilter.LengthFilter(selectedPinLength))
        confirmPinInput.filters = arrayOf(android.text.InputFilter.LengthFilter(selectedPinLength))

        // Update hints
        pinInput.hint = "Enter $selectedPinLength digit PIN"
        confirmPinInput.hint = "Confirm $selectedPinLength digit PIN"

        // Clear previous input
        pinInput.text.clear()
        confirmPinInput.text.clear()
    }
}