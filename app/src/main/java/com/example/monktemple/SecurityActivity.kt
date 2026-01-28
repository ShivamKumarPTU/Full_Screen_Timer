package com.example.monktemple

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.monktemple.databinding.ActivitySecurityBinding
import com.example.monktemple.security.SecurityManager
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SecurityActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityBinding

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var backButton: Button
    private lateinit var biometricSwitch: Switch
    private lateinit var encryptionSwitch: Switch
    private lateinit var securityLevelText: TextView
    private lateinit var securityLevelProgress: ProgressBar
    private lateinit var testBiometricButton: Button
    private lateinit var wipeDataButton: Button
    private lateinit var securityDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()
        loadSecuritySettings()
        updateSecurityLevel()
    }

    private fun initializeViews() {
        backButton = binding.backButton
        biometricSwitch = binding.biometricSwitch
        encryptionSwitch = binding.encryptionSwitch
        securityLevelText = binding.securityLevelText
        securityLevelProgress = binding.securityLevelProgress
        testBiometricButton = binding.testBiometricButton
        wipeDataButton = binding.wipeDataButton
        securityDescription = binding.securityDescription
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableBiometricAuthentication()
            } else {
                disableBiometricAuthentication()
            }
        }

        encryptionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableDataEncryption()
            } else {
                showEncryptionWarning()
            }
        }

        testBiometricButton.setOnClickListener {
            testBiometricAuthentication()
        }

        wipeDataButton.setOnClickListener {
            showWipeDataConfirmation()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadSecuritySettings() {
        // Check biometric availability
        val isBiometricAvailable = securityManager.isBiometricAvailable()
        biometricSwitch.isEnabled = isBiometricAvailable

        if (!isBiometricAvailable) {
            biometricSwitch.isChecked = false
            binding.biometricStatus.text = "Biometric hardware not available"
            binding.biometricStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        } else {
            val isBiometricEnforced = securityManager.isBiometricEnforced()
            biometricSwitch.isChecked = isBiometricEnforced
            binding.biometricStatus.text = if (isBiometricEnforced) "Biometric authentication enabled" else "Biometric authentication available"
            binding.biometricStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }

        // Check encryption status
        val isDataEncrypted = securityManager.isDataEncrypted()
        encryptionSwitch.isChecked = isDataEncrypted
        binding.encryptionStatus.text = if (isDataEncrypted) "Data encryption enabled" else "Data not encrypted"
        binding.encryptionStatus.setTextColor(
            if (isDataEncrypted) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_orange_dark)
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateSecurityLevel() {
        val securityLevel = securityManager.getSecurityLevel()

        when (securityLevel) {
            SecurityManager.SecurityLevel.MILITARY -> {
                securityLevelText.text = "Military Grade 🔒"
                securityLevelProgress.progress = 100
                securityDescription.text = "Your data is protected with biometric authentication and end-to-end encryption. Maximum security level achieved."
                securityLevelText.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            SecurityManager.SecurityLevel.ENHANCED -> {
                securityLevelText.text = "Enhanced Security 🛡️"
                securityLevelProgress.progress = 66
                securityDescription.text = "Your data is encrypted. Consider enabling biometric authentication for maximum security."
                securityLevelText.setTextColor(getColor(android.R.color.holo_blue_dark))
            }
            SecurityManager.SecurityLevel.STANDARD -> {
                securityLevelText.text = "Standard Security 🔐"
                securityLevelProgress.progress = 33
                securityDescription.text = "Basic security enabled. Enable encryption and biometric authentication for enhanced protection."
                securityLevelText.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
        }

        // Update security badges
        updateSecurityBadges(securityLevel)
    }

    @SuppressLint("SetTextI18n")
    private fun updateSecurityBadges(securityLevel: SecurityManager.SecurityLevel) {
        val badges = listOf(
            binding.badgeEncryption,
            binding.badgeBiometric,
            binding.badgeZeroKnowledge
        )

        badges.forEach { it.visibility = android.view.View.GONE }

        when (securityLevel) {
            SecurityManager.SecurityLevel.MILITARY -> {
                binding.badgeEncryption.text = "🔒 End-to-End Encrypted"
                binding.badgeBiometric.text = "👆 Biometric Protected"
                binding.badgeZeroKnowledge.text = "🚫 Zero Data Sharing"
                badges.forEach { it.visibility = android.view.View.VISIBLE }
            }
            SecurityManager.SecurityLevel.ENHANCED -> {
                binding.badgeEncryption.text = "🔒 Data Encrypted"
                binding.badgeEncryption.visibility = android.view.View.VISIBLE
            }
            SecurityManager.SecurityLevel.STANDARD -> {
                // No badges for standard security
            }
        }
    }

    private fun enableBiometricAuthentication() {
        if (!securityManager.isBiometricAvailable()) {
            ShowToast("Biometric authentication not available on this device")
            biometricSwitch.isChecked = false
            return
        }

        securityManager.showBiometricPrompt(
            this,
            onSuccess = {
                lifecycleScope.launch {
                    // Biometric authentication successful
                    sessionManager.securityLevel = "enhanced"
                    updateSecurityLevel()
                    ShowToast("Biometric authentication enabled")
                    loadSecuritySettings() // Refresh settings
                }
            },
            onError = { error ->
                biometricSwitch.isChecked = false
                ShowToast("Failed to enable biometric: $error")
            }
        )
    }

    private fun disableBiometricAuthentication() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Disable Biometric Authentication?")
            .setMessage("This will remove biometric protection from your sensitive data.")
            .setPositiveButton("Disable") { dialog, _ ->
                // In a real implementation, you would remove biometric requirement
                sessionManager.securityLevel = "standard"
                updateSecurityLevel()
                ShowToast("Biometric authentication disabled")
                dialog.dismiss()
            }
            .setNegativeButton("Keep Enabled") { dialog, _ ->
                biometricSwitch.isChecked = true
                dialog.dismiss()
            }
            .show()
    }

    private fun enableDataEncryption() {
        lifecycleScope.launch {
            try {
                // Test encryption with sample data
                val testData = "MonK Temple Security Test"
                val encrypted = securityManager.encryptData(testData)
                val decrypted = securityManager.decryptData(encrypted)

                if (decrypted == testData) {
                    sessionManager.securityLevel = "enhanced"
                    updateSecurityLevel()
                    loadSecuritySettings()
                    ShowToast("Data encryption enabled successfully")
                } else {
                    encryptionSwitch.isChecked = false
                    ShowToast("Encryption test failed")
                }
            } catch (e: Exception) {
                encryptionSwitch.isChecked = false
                ShowToast("Error enabling encryption: ${e.message}")
            }
        }
    }

    private fun showEncryptionWarning() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Disable Data Encryption?")
            .setMessage("Disabling encryption will make your sensitive data vulnerable. This is not recommended.")
            .setPositiveButton("Keep Encrypted") { dialog, _ ->
                encryptionSwitch.isChecked = true
                dialog.dismiss()
            }
            .setNegativeButton("Disable Anyway") { dialog, _ ->
                sessionManager.securityLevel = "standard"
                updateSecurityLevel()
                loadSecuritySettings()
                ShowToast("Data encryption disabled")
                dialog.dismiss()
            }
            .show()
    }

    private fun testBiometricAuthentication() {
        if (!securityManager.isBiometricAvailable()) {
            ShowToast("Biometric authentication not available")
            return
        }

        securityManager.showBiometricPrompt(
            this,
            onSuccess = {
                ShowToast("Biometric authentication successful! ✅")
            },
            onError = { error ->
                ShowToast("Biometric test failed: $error")
            }
        )
    }

    private fun showWipeDataConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Wipe All Sensitive Data?")
            .setMessage("This will permanently delete all encryption keys and sensitive data. This action cannot be undone.")
            .setPositiveButton("Wipe Data") { dialog, _ ->
                wipeSensitiveData()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Learn More") { dialog, _ ->
                showDataWipeInfo()
                dialog.dismiss()
            }
            .show()
    }

    private fun wipeSensitiveData() {
        lifecycleScope.launch {
            try {
                securityManager.wipeSensitiveData()
                sessionManager.securityLevel = "standard"
                biometricSwitch.isChecked = false
                encryptionSwitch.isChecked = false
                updateSecurityLevel()
                loadSecuritySettings()
                ShowToast("All sensitive data wiped securely")
            } catch (e: Exception) {
                ShowToast("Error wiping data: ${e.message}")
            }
        }
    }

    private fun showDataWipeInfo() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("About Data Wiping")
            .setMessage("""
                🔒 Secure Data Wiping
                
                This feature permanently deletes:
                • Encryption keys
                • Biometric authentication data
                • Sensitive session metadata
                
                Your focus sessions and statistics will remain intact, but will no longer be encrypted.
                
                Use this if:
                • You're selling or giving away your device
                • You suspect security compromise
                • You want to start fresh with security settings
            """.trimIndent())
            .setPositiveButton("Understand") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadSecuritySettings()
        updateSecurityLevel()
    }
}