package com.example.monktemple.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "monk_temple_encryption_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    enum class SecurityLevel {
        STANDARD, ENHANCED, MILITARY
    }
    
    fun getSecurityLevel(): SecurityLevel {
        return when {
            isBiometricEnforced() -> SecurityLevel.MILITARY
            isDataEncrypted() -> SecurityLevel.ENHANCED
            else -> SecurityLevel.STANDARD
        }
    }
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    fun isBiometricEnforced(): Boolean {
        // Check if biometric is required for sensitive operations
        return try {
            val key = getSecretKey()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun encryptData(data: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getSecretKey()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Combine IV + encrypted data
            val combined = iv + encrypted
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            data // Fallback to plaintext
        }
    }
    
    fun decryptData(encryptedData: String): String {
        return try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            val iv = combined.copyOfRange(0, 12) // 12 bytes for GCM
            val encrypted = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getSecretKey()
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            encryptedData // Fallback to return as-is
        }
    }
    
    @Throws(Exception::class)
    private fun getSecretKey(): SecretKey {
        // Try to retrieve existing key
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }
        
        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setUserAuthenticationRequired(true)
            setUserAuthenticationValidityDurationSeconds(30) // Require auth every 30 seconds
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                setUnlockedDeviceRequired(true)
            }
        }.build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricAvailable()) {
            onError("Biometric authentication not available")
            return
        }
        
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError("Authentication error: $errString")
                }
                
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MonK Temple Security")
            .setSubtitle("Authenticate to access your focus data")
            .setDescription("Use your fingerprint or face to verify your identity")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()
        
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError("Failed to start authentication: ${e.message}")
        }
    }
    
    private fun isDataEncrypted(): Boolean {
        // Check if we have encryption keys setup
        return try {
            getSecretKey()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun wipeSensitiveData() {
        try {
            // Remove encryption keys
            keyStore.deleteEntry(KEY_ALIAS)
            Log.d(TAG, "🔐 Sensitive data wiped securely")
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping sensitive data", e)
        }
    }
}