package com.example.monktemple

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.monktemple.RoomUser.UserViewModel
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import com.example.monktemple.databinding.ActivityLoginscreenBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Loginscreen : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginscreenBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sessionManager: SessionManager
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var userViewModel: UserViewModel
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Initialization ---
        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        userViewModel = androidx.lifecycle.ViewModelProvider(
            this,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[UserViewModel::class.java]
        userManager = UserManager(sessionManager)

        // Debug: Check what user data we have on app start
        Log.d("LoginScreen", "=== APP START ===")
        sessionManager.debugPrintAllData()

        // Check if user is already logged in
        if (sessionManager.isLoggedIn) {
            val currentUserId = userManager.getCurrentUserID()
            Log.d("LoginScreen", "User already logged in with UID: $currentUserId")

            if (!currentUserId.isNullOrEmpty()) {
                // User is logged in and we have their UID - proceed directly
                handleSuccessfulLogic(
                    currentUserId,
                    currentUserId,
                    sessionManager.getUserName(),
                    sessionManager.getUserEmail(),
                    sessionManager.getProfileImageUri()
                )
                return
            }
        }

        // Check if we have preserved user data from previous session
        val preservedUid = sessionManager.getFirebaseUid()
        if (preservedUid != null) {
            Log.d("LoginScreen", "Found preserved user data for UID: $preservedUid")
            Log.d("LoginScreen", "User needs to log in but their data is preserved in database")
        }

        // --- UI Setup ---
        setupSignUpText()
        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("LoginScreen", "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.w("LoginScreen", "Google sign in failed", e)
                    handleGoogleSignInError(e)
                }
            } else {
                Toast.makeText(this, "Google Sign-In was cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            handleEmailPasswordLogin()
        }

        binding.signInWithGoogle.setOnClickListener {
            showGoogleAccountPicker()
        }

        binding.createAccountText.setOnClickListener {
            startActivity(Intent(this, signUpActivity::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, forgot_pass::class.java))
        }

        setupPasswordToggle()
    }

    private fun showGoogleAccountPicker() {
        // Method 1: Simple approach - just launch sign-in intent
        /*
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
*/
        // Method 2: If Method 1 doesn't work, use this (force sign-out first):

        googleSignInClient.signOut().addOnCompleteListener(this) {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

    }

    private fun handleGoogleSignInError(e: ApiException) {
        when (e.statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show()
            }
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> {
                Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
            }
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> {
                Toast.makeText(this, "Sign in already in progress", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun handleEmailPasswordLogin() {
        val userEmail = binding.emailInputField.text.toString().trim()
        val userPassword = binding.passwordInputField.text.toString().trim()

        if (userEmail.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        lifecycleScope.launch {
                            handleUserLogin(firebaseUser)
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed. Please check credentials.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("LoginScreen", "signInWithCredential:success - User: ${user?.email}")
                    user?.let { firebaseUser ->
                        lifecycleScope.launch {
                            handleUserLogin(firebaseUser)
                        }
                    }
                } else {
                    Log.w("LoginScreen", "signInWithCredential:failure", task.exception)
                    val errorMessage = when {
                        task.exception?.message?.contains("NETWORK_ERROR") == true -> "Network error. Check your connection."
                        task.exception?.message?.contains("INVALID_CREDENTIAL") == true -> "Invalid credentials. Try again."
                        else -> "Google authentication failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private suspend fun handleUserLogin(firebaseUser: FirebaseUser) {
        try {
            Log.d("LoginScreen", "=== HANDLING USER LOGIN ===")

            // Check if we're recovering from a sign-out (data should be preserved)
            val preservedUid = sessionManager.getFirebaseUid()
            if (preservedUid != null && preservedUid == firebaseUser.uid) {
                Log.d("LoginScreen", "🔍 Recovering preserved user data for UID: $preservedUid")

                // Data should already be preserved, just update login state
                sessionManager.isLoggedIn = true
                sessionManager.isFirstTime=false
                sessionManager.updateLastLoginTime()

                // Verify the preserved data
                val userName = sessionManager.getUserName() ?: firebaseUser.displayName ?: "User"
                val userEmail = sessionManager.getUserEmail() ?: firebaseUser.email ?: ""

                Log.d("LoginScreen", "✅ Using preserved user data:")
                Log.d("LoginScreen", "  - Name: $userName")
                Log.d("LoginScreen", "  - Email: $userEmail")
            } else {
                Log.d("LoginScreen", "🆕 New login or different user")
                // Use your existing login logic
                userManager.handleUserLogin(firebaseUser)
                userViewModel.handleUserLogin(firebaseUser)
            }

            withContext(Dispatchers.Main) {
                handleSuccessfulLogic(
                    firebaseUser.uid,
                    firebaseUser.uid,
                    sessionManager.getUserName(),
                    sessionManager.getUserEmail(),
                    sessionManager.getProfileImageUri()
                )
            }
        } catch (e: Exception) {
            Log.e("LoginScreen", "❌ Error handling user login", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Loginscreen, "Login processing failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSuccessfulLogic(
        persistentUserId: String?,
        firebaseUid: String?,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ) {
        // All data is already saved in SessionManager by UserManager.handleUserLogin()

        Log.d("LoginScreen", "=== LOGIN SUCCESSFUL ===")
        Log.d("LoginScreen", "Firebase UID: $firebaseUid")
        Log.d("LoginScreen", "User Name: ${sessionManager.getUserName()}")
        Log.d("LoginScreen", "User Email: ${sessionManager.getUserEmail()}")

        // Navigate to appropriate screen
        if (sessionManager.isAuthRequired) {
            startActivity(Intent(this@Loginscreen, AuthenticationActivity::class.java))
        } else {
            startActivity(Intent(this@Loginscreen, Timer::class.java))
        }
        finish()
    }

    // --- Helper UI Functions ---
    private fun setupSignUpText() {
        val fullText = "Don't have an account? Sign Up"
        val spannable = SpannableString(fullText)
        val signUpStart = fullText.indexOf("Sign Up")

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.blue)),
            signUpStart,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            signUpStart,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.createAccountText.text = spannable
    }

    private fun setupPasswordToggle() {
        var isPassVisible = false
        binding.eyeIcon.setOnClickListener {
            isPassVisible = !isPassVisible
            if (isPassVisible) {
                binding.passwordInputField.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.eyeIcon.setImageResource(R.drawable.eyes_on)
            } else {
                binding.passwordInputField.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.eyeIcon.setImageResource(R.drawable.eyes_off)
            }
            binding.passwordInputField.setSelection(binding.passwordInputField.text?.length ?: 0)
        }
    }
}