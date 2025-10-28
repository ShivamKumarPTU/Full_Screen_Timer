package com.example.monktemple.Utlis

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.monktemple.FaceHelper
import com.example.monktemple.Loginscreen
import com.example.monktemple.R
import com.example.monktemple.RoomUser.UserViewModel
import com.example.monktemple.UserManager
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.example.monktemple.data.sync.UserDataManager
import com.example.monktemple.databinding.DialogAuthOptionBinding
import com.example.monktemple.databinding.SignoutdialogBinding
import com.example.monktemple.setPasswordActivity
import com.example.monktemple.setPinActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogManager @Inject constructor(
    private val lifecycleOwner: LifecycleOwner,
    private val sessionManager: SessionManager,
    private val userDataManager: UserDataManager, // Add this
    private val remoteDataSource: FirebaseRemoteDataSource // Add this
) {
    private val context: Context = lifecycleOwner as Context

    private val faceHelper = FaceHelper(context)
    private lateinit var userViewModel: UserViewModel
    private lateinit var userManager: UserManager


    private fun showAuthOptionDialog(
        title: String,
        @DrawableRes iconRes: Int,
        onEnable: () -> Unit,
        onDisable: () -> Unit,
        currentStatus: Boolean = false
    ) {
        val dialog = createDialog()
        val binding = DialogAuthOptionBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.dialogTitle.text = title
        binding.dialogIcon.setImageResource(iconRes)

        updateDialogUI(binding, currentStatus)

        binding.buttonDisable.setOnClickListener {
            onDisable()
            dialog.dismiss()
        }
        binding.buttonEnable.setOnClickListener {
            onEnable()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateDialogUI(binding: DialogAuthOptionBinding, isEnabled: Boolean) {
        if (isEnabled) {
            binding.statusText.text = "Status: Enabled"
            binding.buttonEnable.alpha = 0.6f
        } else {
            binding.statusText.text = "Status: Disabled"
            binding.buttonDisable.alpha = 0.6f
        }
    }

    fun showBiometricAuthenticationDialog() {
        showAuthOptionDialog(
            title = "Enable Biometric Authentication?",
            iconRes = R.drawable.fingerprint_alt_svgrepo_com,
            onEnable = {
                if(sessionManager.isBiometricEnabled){
                    context.ShowToast("Biometric authentication is already enabled.")
                } else {
                    sessionManager.isBiometricEnabled = true
                    updateAuthRequiredStatus() // Use this instead of directly setting
                    context.ShowToast("Biometric authentication enabled.")
                }
            },
            onDisable = {
                if(sessionManager.isBiometricEnabled) {
                    sessionManager.isBiometricEnabled = false
                    updateAuthRequiredStatus()
                    context.ShowToast("Biometric authentication disabled.")
                } else {
                    context.ShowToast("Biometric authentication is already disabled.")
                }
            },
            currentStatus = sessionManager.isBiometricEnabled
        )
    }

    fun showPasswordAuthenticationDialog() {
        showAuthOptionDialog(
            title = "Enable Password Lock?",
            iconRes = R.drawable.password_svgrepo_com,
            onEnable = {
                if(sessionManager.isPasswordEnabled){
                    context.ShowToast("Password authentication is already enabled.")
                } else {
                    sessionManager.isPasswordEnabled = true
                    updateAuthRequiredStatus() // Use this instead of directly setting
                    context.ShowToast("Password authentication enabled.")
                    val intent = Intent(context, setPasswordActivity::class.java)
                    context.startActivity(intent)
                }
            },
            onDisable = {
                if(sessionManager.isPasswordEnabled) {
                    sessionManager.isPasswordEnabled = false
                    updateAuthRequiredStatus()
                    context.ShowToast("Password authentication disabled.")
                } else {
                    context.ShowToast("Password authentication is already disabled.")
                }
            },
            currentStatus = sessionManager.isPasswordEnabled
        )
    }

    fun showPinAuthenticationDialog() {
        showAuthOptionDialog(
            title = "Enable Pin Lock?",
            iconRes = R.drawable.password_lock_solid_svgrepo_com,
            onEnable = {
                if (sessionManager.isPinEnabled) {
                    context.ShowToast("Pin authentication is already enabled.")
                } else {
                    sessionManager.isPinEnabled = true
                    updateAuthRequiredStatus() // Use this instead of directly setting
                    val intent = Intent(context, setPinActivity::class.java)
                    context.startActivity(intent)
                }
            },
            onDisable = {
                if (sessionManager.isPinEnabled) {
                    sessionManager.isPinEnabled = false
                    updateAuthRequiredStatus()
                    context.ShowToast("Pin authentication disabled.")
                } else {
                    context.ShowToast("Pin authentication is already disabled.")
                }
            },
            currentStatus = sessionManager.isPinEnabled
        )
    }
    fun showSignOutDialog() {

        val dialog = createDialog()
        val binding = SignoutdialogBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.signOutCancelButton.setOnClickListener {
            dialog.dismiss()
        }

        binding.signOutContinueButton.setOnClickListener {
            handleFastSignOut(dialog, binding)
        }
        dialog.show()
    }
    /*
    private fun handleSignOut(dialog: Dialog, binding: SignoutdialogBinding) {
        userManager=UserManager(sessionManager)
        binding.signOutContinueButton.isEnabled = false
        binding.signOutContinueButton.text = "Signing out..."

        lifecycleOwner.lifecycleScope.launch {
            try {
                val currentUserId = userManager.getCurrentUserID()
                Log.d("DialogManager", "=== STARTING ENHANCED SIGNOUT ===")
                Log.d("DialogManager", "Current Firebase UID: $currentUserId")

                // Debug: Print current session data
                sessionManager.debugPrintAllData()

                // Step 1: Use UserDataManager's enhanced logout (this preserves data)
                val logoutSuccess = userDataManager.handleUserLogout()

                if (!logoutSuccess) {
                    Log.w("DialogManager", "UserDataManager logout failed, using fallback")
                    // Fallback: Clear only authentication state
                    sessionManager.clearOnlyAuthenticationState()
                }

                // Step 2: Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
                Log.d("DialogManager", "Firebase signout completed")

                // Step 3: Verify Firebase UID is still preserved
                val preservedUid = sessionManager.getFirebaseUid()
                val preservedName = sessionManager.getUserName()
                val preservedEmail = sessionManager.getUserEmail()

                Log.d("DialogManager", "Firebase UID after signout: $preservedUid")
                Log.d("DialogManager", "User name after signout: $preservedName")
                Log.d("DialogManager", "User email after signout: $preservedEmail")

                if (preservedUid != null) {
                    Log.d("DialogManager", "✅ SUCCESS: User data preserved for UID: $preservedUid")
                    context.ShowToast("Signed out successfully - Your data is saved")

                    // Update Firestore sync status
                    remoteDataSource.updateSyncStatus(preservedUid, "LOGOUT_COMPLETE")
                } else {
                    Log.e("DialogManager", "❌ ERROR: Firebase UID was lost during signout")
                    context.ShowToast("Signed out but data preservation failed")
                }

                dialog.dismiss()

                // Navigate to login screen
                (lifecycleOwner as? Activity)?.let { activity ->
                    val intent = Intent(activity, Loginscreen::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                    activity.finish()
                }

            } catch (e: Exception) {
                Log.e("DialogManager", "❌ Sign out error", e)
                context.ShowToast("Sign out failed: ${e.message}")

                withContext(Dispatchers.Main) {
                    binding.signOutContinueButton.isEnabled = true
                    binding.signOutContinueButton.text = "Continue"
                }
                dialog.dismiss()
            }
        }
    }
*/
    private fun handleFastSignOut(dialog: Dialog, binding: SignoutdialogBinding) {
        binding.signOutContinueButton.isEnabled = false
        binding.signOutContinueButton.text = "Signing out..."

        // Execute on IO thread for better performance
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                minimalSignOut()

                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    navigateToLogin()
                }
            } catch (e: Exception) {
                Log.e("DialogManager", "Sign out error", e)
                withContext(Dispatchers.Main) {
                    context.ShowToast("Sign out failed")
                    binding.signOutContinueButton.isEnabled = true
                    binding.signOutContinueButton.text = "Continue"
                }
            }
        }
    }

    private suspend fun minimalSignOut() {
        Log.d("DialogManager", "=== MINIMAL FAST SIGNOUT ===")

        // 1. Clear only login state (INSTANT)
        sessionManager.isLoggedIn = false

        // 2. Firebase sign-out (usually fast)
        FirebaseAuth.getInstance().signOut()

        Log.d("DialogManager", "Minimal sign-out completed")
    }

    private fun navigateToLogin() {
        context.ShowToast("Signed out successfully")

        // Instant navigation
        (lifecycleOwner as? Activity)?.let { activity ->
            val intent = Intent(activity, Loginscreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            activity.finish()
        }
    }

    private fun createDialog(): Dialog {
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }

    private fun updateAuthRequiredStatus() {
        Log.d("DialogManager", "Auth methods updated - auto-syncing in SessionManager")
    }
}