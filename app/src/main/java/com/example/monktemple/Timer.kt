package com.example.monktemple

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.monktemple.databinding.ActivityTimerBinding
import com.example.monktemple.Utlis.DialogManager
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.monktemple.RoomUser.UserDao
import com.example.monktemple.RoomUser.UserDatabase
import com.example.monktemple.RoomUser.UserViewModel
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.example.monktemple.data.sync.UserDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class Timer : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private val binding: ActivityTimerBinding by lazy {
        ActivityTimerBinding.inflate(layoutInflater)
    }
    // Room Database variables
    private lateinit var userDao:UserDao
    private lateinit var dialogManager: DialogManager
    private lateinit var sessionManager: SessionManager
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var timePicker: TimePicker
    private lateinit var startButton: AppCompatButton
    private lateinit var audioManager: AudioManager
    private lateinit var goalInput: TextView
    private lateinit var userViewModel: UserViewModel
    private lateinit var userManager: UserManager
    private lateinit var userDataManager: UserDataManager
    private lateinit var remoteDataSource: FirebaseRemoteDataSource

    private var lastHour = -1
    private var lastMinute = -1

    // variable declaration for selecting image
    private lateinit var headerIcon: CircleImageView
    private lateinit var headerTitle: TextView
    private lateinit var headerSubTitle: TextView
    private lateinit var editNameIcon: ImageView
    private lateinit var editNameIcon1:ImageView

    // This will launch an activity for the user to select the image
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // This will make sure that the again and again permission for the image uri is granted
                // and this tell the system that it's for read only purpose
                val contentResolver = applicationContext.contentResolver
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                headerIcon.setImageURI(it)
                sessionManager.saveProfileImageUri(it.toString())
            } catch (e: Exception) {
                ShowToast("Error loading image")
                e.printStackTrace()
            }
        }
    }

    // Asking the user to grant permission
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            ShowToast("Permission denied. You can't change profile picture")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        overridePendingTransition(0,0)
        hidestatusbar()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views first
        initializeViews()
        setupFloatingHamburger()
        // Setup other components
        setupTimePicker()
        setupStartButton()
        // function to handle nav header
        setupNavHeader()
    }

    private fun setupFloatingHamburger() {
        // THIS IS THE CLICK LISTENER THAT OPENS THE NAV DRAWER ↓
        binding.floatingHamburger.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_pin -> {
                dialogManager.showPinAuthenticationDialog()
            }

            R.id.nav_fingerprint -> {
                dialogManager.showBiometricAuthenticationDialog()
            }


            R.id.nav_password -> {
                dialogManager.showPasswordAuthenticationDialog()
            }
           R.id.nav_statistics->{
               startActivity(Intent(this,Statistics::class.java))
               finish()
           }

            R.id.nav_signout -> {
                dialogManager.showSignOutDialog()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun initializeViews() {
        remoteDataSource= FirebaseRemoteDataSource()
        userDao = UserDatabase.getDatabase(this).userDao()
        userDataManager = UserDataManager(this,userDao,remoteDataSource)
        remoteDataSource = FirebaseRemoteDataSource()
        sessionManager = SessionManager(this)
        userViewModel= ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[UserViewModel::class.java]
       userManager=UserManager(sessionManager)
     dialogManager= DialogManager(this,sessionManager, userDataManager, remoteDataSource)
        drawerLayout = binding.drawerLayout
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // --- FIX THESE LINES ---
        timePicker = binding.timePicker // Use binding
        startButton = binding.startButton   // Use binding
        goalInput = binding.goalInput     // Use binding
        // --- END OF FIX ---

        // Set initial values
        timePicker.hour=0
       timePicker.minute=0
        timePicker.setIs24HourView(true)


    }


    private fun setupTimePicker() {
        binding.timePicker.setOnTimeChangedListener { _, hourOfDay, minute -> // Use binding
            if (hourOfDay != lastHour || minute != lastMinute) {
                playScrollSound()
                lastHour = hourOfDay
                lastMinute = minute
            }
        }
    }


    private fun setupStartButton() {
        binding.startButton.setOnClickListener { // Use binding
            val hour = binding.timePicker.hour // Use binding
            val minute = binding.timePicker.minute // Use binding
            val totalMinutes = (hour * 60) + minute
            val goalName = binding.goalInput.text.toString().trim() // Use binding

            if (totalMinutes > 0) {
                val intent = Intent(this, TimerScreen::class.java)
                intent.putExtra("time", totalMinutes)
                intent.putExtra("goalName", goalName)
                startActivity(intent)
            } else {
                ShowToast("Please set a valid time")
            }

        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun playScrollSound() {
        try {
            val volume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM).toFloat() /
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)

            val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(10, 50))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10)
                }
            }

            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, volume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun hidestatusbar() {
        // Completely hide the status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun setupNavHeader() {
        val headerView = binding.navView.getHeaderView(0)
        headerIcon = headerView.findViewById(R.id.nav_header_icon)
        headerTitle = headerView.findViewById(R.id.nav_header_title)
        headerSubTitle = headerView.findViewById(R.id.nav_header_subtitle)
        editNameIcon = headerView.findViewById(R.id.edit_name_icon)
        editNameIcon1 = headerView.findViewById(R.id.edit_name_icon1)

        editNameIcon1.setOnClickListener {
            val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            when {
                ContextCompat.checkSelfPermission(this, permissionToRequest) == PackageManager.PERMISSION_GRANTED -> {
                    pickImageLauncher.launch("image/*")
                }
                shouldShowRequestPermissionRationale(permissionToRequest) -> {
                    // Explain why you need the permission
                    AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("This permission is needed to select a profile picture from your gallery")
                        .setPositiveButton("OK") { _, _ ->
                            requestPermissionLauncher.launch(permissionToRequest)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(permissionToRequest)
                }
            }
        }

        editNameIcon.setOnClickListener {
            showEditNameDialog()
        }
        loadUserData()
    }

    private fun loadUserData() {
        // Always set email from session first
        headerSubTitle.text = sessionManager.getUserEmail() ?: "abc@gmail.com"

        // Load name and image data
        loadUserName()
        loadUserImage()
    }

    private fun loadUserName() {
        val savedUserName = sessionManager.getUserName()

        if (!savedUserName.isNullOrEmpty() && savedUserName != "New User" && savedUserName != "Enter Your Name") {
            // User has set a custom name
            headerTitle.text = savedUserName
        } else {
            // Load from database
            loadUserNameFromDatabase()
        }
    }

    private fun loadUserImage() {
        val savedImageUriString = sessionManager.getProfileImageUri()

        if (!savedImageUriString.isNullOrEmpty() && savedImageUriString != "default") {
            // User has set a custom image
            try {
                if (savedImageUriString.startsWith("content://") || savedImageUriString.startsWith("file://")) {
                    headerIcon.setImageURI(savedImageUriString.toUri())
                } else {
                    // It's a default marker, use default drawable
                    headerIcon.setImageResource(R.drawable._4)
                }
            } catch (e: Exception) {
                // Fallback to default image if there's an error
                headerIcon.setImageResource(R.drawable._4)
                sessionManager.saveProfileImageUri("default")
            }
        } else {
            // Load from database or use default
            loadUserImageFromDatabase()
        }
    }

    private fun loadUserNameFromDatabase() {
        val userManager=UserManager(sessionManager)
        val currentUserId = userManager.getCurrentUserID()
        if (!currentUserId.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val userViewModel = ViewModelProvider(
                        this@Timer,
                        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                    )[UserViewModel::class.java]

                    val user = userViewModel.getUserById(currentUserId)

                    withContext(Dispatchers.Main) {
                        user?.let {
                            val userName = it.displayName ?: "Enter Your Name"
                            headerTitle.text = userName
                            // Save to SharedPreferences for future use
                            if (userName != "Enter Your Name") {
                                sessionManager.saveUserName(userName)
                            }
                        } ?: run {
                            // User not found in database, set default
                            headerTitle.text = "Enter Your Name"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Timer", "Error loading user name from database", e)
                    withContext(Dispatchers.Main) {
                        headerTitle.text = "Enter Your Name"
                    }
                }
            }
        } else {
            // No user ID, set default
            headerTitle.text = "Enter Your Name"
        }
    }

    private fun loadUserImageFromDatabase() {
        val currentUserId = sessionManager.getFirebaseUid() // Use Firebase UID instead of local user ID
        if (!currentUserId.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val user = userViewModel.getUserByFirebaseUid(currentUserId)

                    withContext(Dispatchers.Main) {
                        user?.let {
                            val photoUrl = it.photoUrl ?: "default"
                            if (photoUrl != "default" && photoUrl.isNotEmpty() &&
                                (photoUrl.startsWith("content://") || photoUrl.startsWith("file://"))) {
                                try {
                                    headerIcon.setImageURI(photoUrl.toUri())
                                    sessionManager.saveProfileImageUri(photoUrl)
                                } catch (e: Exception) {
                                    setDefaultProfileImage()
                                }
                            } else {
                                setDefaultProfileImage()
                            }
                        } ?: run {
                            // User not found in database, set default
                            setDefaultProfileImage()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Timer", "Error loading user image from database", e)
                    withContext(Dispatchers.Main) {
                        setDefaultProfileImage()
                    }
                }
            }
        } else {
            // No user ID, set default
            setDefaultProfileImage()
        }
    }

    private fun setDefaultProfileImage() {
        headerIcon.setImageResource(R.drawable._4)
        sessionManager.saveProfileImageUri("default")
    }
    private fun showEditNameDialog() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Enter Your Name")

        val input = TextInputEditText(this)
        input.hint = "Your Name"
        input.setText(headerTitle.text)

        // Set layout params to prevent small input field
        val layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(50, 20, 50, 20)
        input.layoutParams = layoutParams

        builder.setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    headerTitle.text = newName
                    sessionManager.saveUserName(newName)
                    ShowToast("Name updated successfully")
                    dialog.dismiss()
                } else {
                    ShowToast("Name cannot be empty")
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }


}