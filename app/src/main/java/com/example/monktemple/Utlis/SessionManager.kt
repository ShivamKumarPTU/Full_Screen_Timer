package com.example.monktemple.Utlis

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val appContext: Context = context.applicationContext // Use application context

    companion object {
        private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
        private const val KEY_USER_NAME = "user_name"
        private const val PREF_NAME = "monk_temple_session"
        private const val KEY_IS_AUTH_REQUIRED = "is_auth_required"
        private const val IS_FIRST_TIME = "is_first_time"
        private const val IS_LOGGED_IN = "is_logged_in"
        private const val ACCESS_TOKEN = "access_token"
        private const val USER_ID = "user_id"
        private const val USER_EMAIL = "user_email"
        private const val LAST_LOGIN_TIME = "last_login_time"
        private const val KEY_IS_BIOMETRIC_ENABLED = "is_biometric_enabled"
        private const val KEY_IS_FACE_UNLOCK_ENABLED = "is_face_unlock_enabled"
        private const val KEY_IS_PASSWORD_ENABLED = "is_password_enabled"
        private const val KEY_IS_PIN_ENABLED = "is_pin_enabled"
        private const val PERSISTENT_USER_ID = "persistent_user_id"
        private const val FIREBASE_UID = "firebase_uid"

        private const val KEY_ZERO_DISTRACTION = "zero_distraction_enabled"
        private const val KEY_OPTIMAL_SESSION_LENGTH = "optimal_session_length"
        private const val KEY_PEAK_HOURS = "peak_productivity_hours"
        private const val KEY_SECURITY_LEVEL = "security_level"
        private const val KEY_CUSTOM_THEME = "custom_theme"
        private const val KEY_TIMER_SOUND = "timer_sound"


    }


    // Enhanced logout that preserves ALL user data
    fun clearOnlyAuthenticationState() {
        // PRESERVE all user identity data - only clear login state
        prefs.edit {
            putBoolean(IS_LOGGED_IN, false)
            remove(ACCESS_TOKEN)
            remove(LAST_LOGIN_TIME)
            // DO NOT remove any user identity data
        }
    }

    // Completely clear user data (only for factory reset)
    fun clearAllUserData() {
        prefs.edit {
            clear()
        }
    }

    // Authentication methods
    var isAuthRequired: Boolean
        get() = prefs.getBoolean(KEY_IS_AUTH_REQUIRED, false)
        private set(value) = prefs.edit {
            putBoolean(KEY_IS_AUTH_REQUIRED, value)
            Log.d("SessionManager", "Setting isAuthRequired to: $value")
        }

    fun resetAllAuthentication() {
        prefs.edit {
            putBoolean(KEY_IS_BIOMETRIC_ENABLED, false)
            putBoolean(KEY_IS_PASSWORD_ENABLED, false)
            putBoolean(KEY_IS_PIN_ENABLED, false)
           // putBoolean(KEY_IS_AUTH_REQUIRED, false)
        }
        updateAuthRequiredStatus()
        Log.d("SessionManager", "All authentication methods reset")
    }

    // Add this method to properly update auth status
    private fun updateAuthRequiredStatus() {
        val isAnyAuthEnabled = isBiometricEnabled || isPasswordEnabled || isPinEnabled
        val currentAuthRequired = prefs.getBoolean(KEY_IS_AUTH_REQUIRED, false)

        if (currentAuthRequired != isAnyAuthEnabled) {
            prefs.edit {
                putBoolean(KEY_IS_AUTH_REQUIRED, isAnyAuthEnabled)
            }
            Log.d("SessionManager", "Auth required updated to: $isAnyAuthEnabled")
        }
    }

    fun isAnyAuthEnabled(): Boolean {
        return isBiometricEnabled  || isPasswordEnabled || isPinEnabled
    }

    var isFirstTime: Boolean
        get() = prefs.getBoolean(IS_FIRST_TIME, true)
        set(value) = prefs.edit { putBoolean(IS_FIRST_TIME, value) }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(IS_LOGGED_IN, false)
        set(value) = prefs.edit { putBoolean(IS_LOGGED_IN, value) }

    // Update all setters to call updateAuthRequiredStatus
    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit {
            putBoolean(KEY_IS_BIOMETRIC_ENABLED, value)
            updateAuthRequiredStatus()
        }

    var isPasswordEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_PASSWORD_ENABLED, false)
        set(value) = prefs.edit {
            putBoolean(KEY_IS_PASSWORD_ENABLED, value)
            updateAuthRequiredStatus()
        }

    var isPinEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_PIN_ENABLED, false)
        set(value) = prefs.edit {
            putBoolean(KEY_IS_PIN_ENABLED, value)
            updateAuthRequiredStatus()
        }


    fun saveAuthToken(token: String?) {
        prefs.edit { putString(ACCESS_TOKEN, token) }
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
    }

    fun saveUserDetails(id: String?, email: String?) {
        prefs.edit {
            putString(USER_ID, id)
            putString(USER_EMAIL, email)
        }
    }

    fun getUserId(): String? {
        return prefs.getString(USER_ID, null)
    }

    fun saveUserEmail(email: String?) {
        prefs.edit { putString(USER_EMAIL, email) }
    }

    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    fun updateLastLoginTime() {
        prefs.edit { putLong(LAST_LOGIN_TIME, System.currentTimeMillis()) }
    }

    fun getLastLoginTime(): Long {
        return prefs.getLong(LAST_LOGIN_TIME, 0L)
    }

    fun factoryReset() {
        prefs.edit { clear() }
    }

    fun saveProfileImageUri(uriString: String?) {
        prefs.edit { putString(KEY_PROFILE_IMAGE_URI, uriString) }
    }

    fun getProfileImageUri(): String? {
        return prefs.getString(KEY_PROFILE_IMAGE_URI, null)
    }

    fun saveUserName(name: String?) {
        prefs.edit { putString(KEY_USER_NAME, name) }
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    // CRITICAL: Save Firebase UID (this is the key to data persistence)
    fun saveFirebaseUid(uid: String) {
        prefs.edit { putString(FIREBASE_UID, uid) }
    }

    fun getFirebaseUid(): String? {
        return prefs.getString(FIREBASE_UID, null)
    }

    fun clearFirebaseUid() {
        prefs.edit { remove(FIREBASE_UID) }
    }

    // Persistent user ID for anonymous users
    fun savePersistentUserId(userId: String) {
        prefs.edit { putString(PERSISTENT_USER_ID, userId) }
    }

    fun getPersistentUserId(): String? {
        return prefs.getString(PERSISTENT_USER_ID, null)
    }

    // Debug method to check what data is stored
    fun debugPrintAllData() {
        val allEntries = prefs.all
        println("=== SessionManager Debug ===")
        allEntries.forEach { (key, value) ->
            println("$key = $value")
        }
        println("===========================")
    }
    // Add this method to ensure auth status is synchronized
    fun syncAuthenticationStatus() {
        val isAnyAuthEnabled = isBiometricEnabled || isPasswordEnabled || isPinEnabled
        if (isAuthRequired != isAnyAuthEnabled) {
            isAuthRequired = isAnyAuthEnabled
            Log.d("SessionManager", "Synced auth status: $isAuthRequired")
        }
    }

    /**
     * Mark that user has completed first-time experience (signup or first login)
     */
    fun completeFirstTimeExperience() {
        prefs.edit {
            putBoolean(IS_FIRST_TIME, false)
        }
        Log.d("SessionManager", "First time experience completed")
    }

    /**
     * Smart check for first time - considers if user data exists
     */
    fun isTrulyFirstTime(): Boolean {
        val hasUserData = getFirebaseUid() != null || getUserId() != null
        val isFirstTimeFlag = prefs.getBoolean(IS_FIRST_TIME, true)

        // If we have user data but flag says first time, fix the inconsistency
        if (hasUserData && isFirstTimeFlag) {
            completeFirstTimeExperience()
            return false
        }

        return isFirstTimeFlag && !hasUserData
    }
    fun enableBiometricAuth(): Boolean {
        // 1. Get the BiometricManager from the system service (non-deprecated)
        val biometricManager = BiometricManager.from(appContext)

        // 2. Check if the device can authenticate
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)

        return when (canAuth) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("SessionManager", "Biometric authentication can be enabled.")
                // 3. If successful, update the flag using the property setter
                isBiometricEnabled = true
                true // Return true to indicate success
            }
            else -> {
                Log.w("SessionManager", "Biometric authentication not available. Code: $canAuth")
                // Ensure the flag is disabled if check fails
                isBiometricEnabled = false
                false // Return false to indicate failure
            }
        }
    }



    // Data encryption status
    fun isDataEncrypted(): Boolean {
        // Check if sensitive data is properly encrypted
        return !fetchAuthToken().isNullOrEmpty() &&
                !getFirebaseUid().isNullOrEmpty() &&
                prefs.contains(KEY_IS_AUTH_REQUIRED)
    }

    // Add these properties to SessionManager class
    var isZeroDistractionEnabled: Boolean
        get() = prefs.getBoolean(KEY_ZERO_DISTRACTION, false)
        set(value) = prefs.edit { putBoolean(KEY_ZERO_DISTRACTION, value) }

    var optimalSessionLength: Int
        get() = prefs.getInt(KEY_OPTIMAL_SESSION_LENGTH, 25)
        set(value) = prefs.edit { putInt(KEY_OPTIMAL_SESSION_LENGTH, value) }

    var peakProductivityHours: String
        get() = prefs.getString(KEY_PEAK_HOURS, "9,14,16") ?: "9,14,16"
        set(value) = prefs.edit { putString(KEY_PEAK_HOURS, value) }

    var securityLevel: String
        get() = prefs.getString(KEY_SECURITY_LEVEL, "standard") ?: "standard"
        set(value) = prefs.edit { putString(KEY_SECURITY_LEVEL, value) }

    var customTheme: String
        get() = prefs.getString(KEY_CUSTOM_THEME, "default") ?: "default"
        set(value) = prefs.edit { putString(KEY_CUSTOM_THEME, value) }

    var timerSound: String
        get() = prefs.getString(KEY_TIMER_SOUND, "default") ?: "default"
        set(value) = prefs.edit { putString(KEY_TIMER_SOUND, value) }
}