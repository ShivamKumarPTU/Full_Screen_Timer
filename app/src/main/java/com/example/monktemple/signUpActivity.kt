@file:Suppress("DEPRECATION")

package com.example.monktemple

import android.app.ProgressDialog
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
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monktemple.RoomUser.User
import com.example.monktemple.RoomUser.UserViewModel
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.databinding.ActivitySignUpBinding
import com.example.monktemple.Utlis.ShowToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class signUpActivity : AppCompatActivity() {
    private val viewBinding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private lateinit var userViewModel: UserViewModel
    private lateinit var userManager: UserManager
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        // Initialize components
        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        userViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[UserViewModel::class.java]
        userManager = UserManager(sessionManager)

        // Check if user is already logged in
        if (sessionManager.isLoggedIn) {
            redirectToMainActivity()
            return
        }

        setupSignInText()
        setupPasswordFields()
        setupSignUpButton()
    }

    private fun setupSignInText() {
        val signInText = viewBinding.signInText
        val fullText = "Already have an account? Sign In"
        val spannable = SpannableString(fullText)
        val signInStart = fullText.indexOf("Sign In")

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.blue)),
            signInStart,
            fullText.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            signInStart,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        signInText.text = spannable
        signInText.setOnClickListener {
            startActivity(Intent(this, Loginscreen::class.java))
            finish()
        }
    }

    private fun setupPasswordFields() {
        setupPasswordToggle(viewBinding.passEyeIcon, viewBinding.enterPassword)
        setupPasswordToggle(viewBinding.confirmPassEyeIcon, viewBinding.confirmPassword)
    }

    private fun setupPasswordToggle(eyeButton: ImageButton, editText: EditText) {
        var isVisible = false
        eyeButton.setOnClickListener {
            if (isVisible) {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                eyeButton.setImageResource(R.drawable.eyes_off)
            } else {
                editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                eyeButton.setImageResource(R.drawable.eyes_on)
            }
            isVisible = !isVisible
            editText.setSelection(editText.text?.length ?: 0)
        }
    }

    private fun setupSignUpButton() {
        viewBinding.signUpButton.setOnClickListener {
            val emailText = viewBinding.enterEmail.text.toString().trim()
            val passwordText = viewBinding.enterPassword.text.toString().trim()
            val confirmPassText = viewBinding.confirmPassword.text.toString().trim()

            if (!validateInputs(emailText, passwordText, confirmPassText)) {
                return@setOnClickListener
            }

            createUserWithEmail(emailText, passwordText)
        }
    }

    private fun validateInputs(email: String, password: String, confirmPass: String): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ShowToast("Please enter a valid email address")
            return false
        }
        if (password.isEmpty() || confirmPass.isEmpty()) {
            ShowToast("Please fill all the fields")
            return false
        }
        if (password.length < 6) {
            ShowToast("Password should be at least 6 characters")
            return false
        }
        if (password != confirmPass) {
            ShowToast("Password and Confirm Password do not match")
            return false
        }
        return true
    }

    private fun createUserWithEmail(email: String, password: String) {
        showLoading("Creating Account...")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        handleSuccessfulSignUp(firebaseUser)
                    }
                } else {
                    hideLoading()
                    handleSignUpError(task.exception)
                }
            }
    }

    private fun handleSuccessfulSignUp(firebaseUser: FirebaseUser) {
        // MARK THIS AS CRITICAL: User is no longer first-time
        sessionManager.completeFirstTimeExperience()

        // Use UserManager to handle login permanently
        userManager.handleUserLogin(firebaseUser)

        val currentUserId = userManager.getUserId(firebaseUser)

        // Save user to Room database
        val userToSave = User(
            userId = currentUserId ?: firebaseUser.uid,
            firebaseUid = firebaseUser.uid,
            displayName = firebaseUser.displayName ?: "User",
            email = firebaseUser.email ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: "R.drawable._4"
        )

        userViewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                userViewModel.saveUser(userToSave)

                // Handle user login in ViewModel for data consistency
                userViewModel.handleUserLogin(firebaseUser)

                withContext(Dispatchers.Main) {
                    hideLoading()
                    sessionManager.isLoggedIn = true
                    sessionManager.saveAuthToken(currentUserId)

                    sendEmailVerification(firebaseUser)
                    ShowToast("Sign Up Successful")

                    // For first-time signup, go directly to Timer (no auth setup yet)
                    Log.d("SignUpActivity", "🎉 Signup complete - navigating to Timer")
                    redirectToMainActivity()
                }
            } catch (e: Exception) {
                Log.e("SignUpActivity", "Error saving user to database", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    ShowToast("Account created but error saving user data")
                    redirectToMainActivity() // Still redirect as account was created
                }
            }
        }
    }
    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    ShowToast("Verification email sent to ${user.email}")
                } else {
                    ShowToast("Failed to send verification email")
                }
            }
    }

    private fun handleSignUpError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthUserCollisionException -> ShowToast("Email is already registered")
            else -> ShowToast("Sign Up failed: ${exception?.message ?: "Unknown error"}")
        }
    }

    private fun redirectToMainActivity() {
        startActivity(Intent(this, Timer::class.java))
        finishAffinity()
    }

    private fun showLoading(message: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this).apply {
                this.setMessage(message)
                this.setCancelable(false)
            }
        }
        if (!isFinishing && progressDialog?.isShowing == false) {
            progressDialog?.show()
        }
    }

    private fun hideLoading() {
        if (!isFinishing && progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoading()
    }
}