package com.example.monktemple

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class FaceHelper (private val context: Context) {

    // On many devices, BIOMETRIC_WEAK handles both Face and Fingerprint.
    // We use this and rely on the device to pick the enrolled weak biometric.
    private val WEAK_BIOMETRIC_AUTHENTICATOR = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun isFaceUnlockAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        // Check if the device has *any* weak biometric that includes Face/Fingerprint
        val result = biometricManager.canAuthenticate(WEAK_BIOMETRIC_AUTHENTICATOR)

        // Also check if device has face hardware (though BiometricManager handles the final check)
        val hasHardware = biometricManager.canAuthenticate(WEAK_BIOMETRIC_AUTHENTICATOR) !=
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

        return result == BiometricManager.BIOMETRIC_SUCCESS && hasHardware
    }

    fun showFaceUnlockDialog(
        activity: AppCompatActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!isFaceUnlockAvailable()) {
            val errorMsg = getFaceUnlockErrorDetails()
            onError("Face unlock unavailable: $errorMsg")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // ERROR_USER_CANCELED or ERROR_NEGATIVE_BUTTON means the user chose not to authenticate
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError("Face unlock cancelled by user.")
                    } else {
                        onError("Face unlock error: $errString")
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        // Refined Prompt Info
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Monk Temple Face Unlock") // Clear title
            .setSubtitle("Use your face to access the app") // Clear subtitle
            .setDescription("Look at your device to authenticate.")
            .setNegativeButtonText("Use PIN/Password") // Provide a clear fallback option
            .setAllowedAuthenticators(WEAK_BIOMETRIC_AUTHENTICATOR)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
            Log.d("FaceHelper", "Face unlock prompt shown successfully")
        } catch (e: Exception) {
            Log.e("FaceHelper", "Error showing face unlock prompt: ${e.message}")
            onError("Failed to start face unlock: ${e.message}")
        }
    }

    private fun getFaceUnlockErrorDetails(): String {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(WEAK_BIOMETRIC_AUTHENTICATOR)

        return when (result) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric data enrolled. Please set up face or fingerprint in device settings."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware temporarily unavailable."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Device doesn't support biometrics."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required."
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Biometrics not supported on this device."
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unable to determine biometric status."
            else -> "Unknown error (Code: $result)"
        }
    }
}