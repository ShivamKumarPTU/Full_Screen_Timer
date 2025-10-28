package com.example.monktemple

import com.example.monktemple.Utlis.SessionManager
import com.google.firebase.auth.FirebaseUser


class UserManager(private val sessionManager: SessionManager) {

    fun getUserId(firebaseUser: FirebaseUser?): String? {
        return firebaseUser?.uid ?: sessionManager.getFirebaseUid()
    }

    fun getCurrentUserID(): String? {
        return sessionManager.getFirebaseUid()
    }

    fun isReturningUser(): Boolean {
        return !sessionManager.getFirebaseUid().isNullOrEmpty()
    }

    fun handleUserLogin(firebaseUser: FirebaseUser) {
        // CRITICAL: Save Firebase UID for data persistence
        sessionManager.saveFirebaseUid(firebaseUser.uid)
        sessionManager.saveUserEmail(firebaseUser.email ?: "")
        sessionManager.saveUserName(firebaseUser.displayName ?: "User")
        firebaseUser.photoUrl?.toString()?.let {
            sessionManager.saveProfileImageUri(it)
        }
        sessionManager.isLoggedIn = true
        sessionManager.updateLastLoginTime()

        // Debug log
        println("✅ UserManager: User logged in - UID: ${firebaseUser.uid}")
    }

    // Check if we have a preserved user ID for data linking
    fun hasPreservedUserData(): Boolean {
        return !sessionManager.getFirebaseUid().isNullOrEmpty()
    }

    // Get the preserved user ID for data loading
    fun getPreservedUserId(): String? {
        return sessionManager.getFirebaseUid()
    }
}