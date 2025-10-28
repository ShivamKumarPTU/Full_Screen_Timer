package com.example.monktemple

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class FingerprintHelper(private val context: Context) {

    fun isFingerprintAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        Log.d("FingerprintHelper", "Biometric availability result: $result")

        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("FingerprintHelper", "Fingerprint available: BIOMETRIC_SUCCESS")
                true
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.d("FingerprintHelper", "Fingerprint unavailable: No fingerprints enrolled")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.d("FingerprintHelper", "Fingerprint unavailable: Hardware unavailable")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.d("FingerprintHelper", "Fingerprint unavailable: No hardware")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.d("FingerprintHelper", "Fingerprint unavailable: Security update required")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.d("FingerprintHelper", "Fingerprint unavailable: Unsupported")
                false
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.d("FingerprintHelper", "Fingerprint status unknown")
                false
            }
            else -> {
                Log.d("FingerprintHelper", "Fingerprint unavailable: Unknown reason")
                false
            }
        }
    }

    // Alternative method with more flexible authentication types
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)

        // Try BIOMETRIC_STRONG first (fingerprint)
        val strongResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (strongResult == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.d("FingerprintHelper", "BIOMETRIC_STRONG available")
            return true
        }

        // Try BIOMETRIC_WEAK as fallback
        val weakResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (weakResult == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.d("FingerprintHelper", "BIOMETRIC_WEAK available")
            return true
        }

        // Try any biometric
        val anyResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (anyResult == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.d("FingerprintHelper", "Any biometric available")
            return true
        }

        Log.d("FingerprintHelper", "No biometric available. Strong: $strongResult, Weak: $weakResult")
        return false
    }

    fun showFingerprintDialog(
        activity: AppCompatActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        // Use the more flexible check
        if (!isFingerprintAvailable()) {
            val errorMsg = getBiometricErrorDetails()
            onError("Biometric authentication unavailable: $errorMsg")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("FingerprintHelper", "Authentication error: $errorCode - $errString")
                    onError("Error: $errString (Code: $errorCode)")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("FingerprintHelper", "Authentication successful")
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d("FingerprintHelper", "Authentication failed")
                    onFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MonK Temple")
            .setSubtitle("Verify your identity")
            .setDescription("Use your fingerprint or device credentials")
            //  .setNegativeButtonText("Cancel")
            // Allow both strong biometric and device credentials
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
            Log.d("FingerprintHelper", "Biometric prompt shown successfully")
        } catch (e: Exception) {
            Log.e("FingerprintHelper", "Error showing biometric prompt: ${e.message}")
            onError("Failed to start authentication: ${e.message}")
        }
    }

    private fun getBiometricErrorDetails(): String {
        
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        return when (result) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprints enrolled. Please add fingerprints in device settings."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware temporarily unavailable."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Device doesn't have biometric hardware."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required for biometric features."
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Biometric authentication not supported."
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unable to determine biometric status."
            else -> "Unknown error (Code: $result)"
        }
    }
}