package com.example.monktemple

<<<<<<< HEAD
import com.example.monktemple.RoomUser.UserViewModel
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.WindowManager
=======
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
<<<<<<< HEAD
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monktemple.RoomUser.UserClass
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.databinding.ActivityTimerScreenBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class TimerScreen : AppCompatActivity() {
    private var isCancelled = false
    private lateinit var timerText: TextView
    private lateinit var pauseButton: Button
    private lateinit var cancelButton: Button
    private lateinit var goalInputName: AppCompatEditText
=======
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class TimerScreen : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var pauseButton: Button
    private lateinit var cancelButton: Button
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201

    private var countDownTimer: CountDownTimer? = null
    private var totalTimeMillis: Long = 0L
    private var timeLeftMillis: Long = 0L
    private var timerRunning = false

<<<<<<< HEAD
    private val binding by lazy{
        ActivityTimerScreenBinding.inflate(layoutInflater)
    }
    private lateinit var mUserViewModel: UserViewModel
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
=======
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timer_screen)
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

<<<<<<< HEAD
        firebaseAuth = FirebaseAuth.getInstance()
        mUserViewModel = ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application))[UserViewModel::class.java]
        sessionManager = SessionManager(this)
        userManager = UserManager(sessionManager)

        timerText = findViewById(R.id.textView)
        pauseButton = findViewById(R.id.button)
        cancelButton = findViewById(R.id.button2)
        goalInputName = findViewById(R.id.goalInputTimerScreenText)

        val goalName = intent.getStringExtra("goalName")
        if (!goalName.isNullOrEmpty()) {
            binding.goalInputTimerScreenText.setText(goalName)
        } else {
            binding.goalInputTimerScreenText.setText("Focus Time")
        }

=======
        // Bind UI
        timerText = findViewById(R.id.textView)
        pauseButton = findViewById(R.id.button)
        cancelButton = findViewById(R.id.button2)

        // Get duration (in minutes) from Intent and convert to milliseconds
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
        val durationInMinutes = intent.getIntExtra("time", 0)
        totalTimeMillis = durationInMinutes.toLong() * 60 * 1000L
        timeLeftMillis = totalTimeMillis

        updateTimerText()
<<<<<<< HEAD
=======

        // Start timer immediately
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
        startTimer()

        pauseButton.setOnClickListener {
            if (timerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        cancelButton.setOnClickListener {
            cancelTimer()
<<<<<<< HEAD
=======
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
            finish()
        }
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
<<<<<<< HEAD
                if(!isCancelled){
                    saveMeditationSession(totalTimeMillis,"COMPLETED")
                }

                timerRunning = false
                timeLeftMillis = 0
                binding.textView.text="00:00:00"
                Toast.makeText(this@TimerScreen, "Timer Finished", Toast.LENGTH_SHORT).show()
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                NotificationHelper.showTimerFinishedNotification(this@TimerScreen)
                startActivity(Intent(this@TimerScreen, Timer::class.java))
                overridePendingTransition(0,0)
                finish()
            }
        }.start()
=======
                timerRunning = false
                timeLeftMillis = 0
                updateTimerText()
                Toast.makeText(this@TimerScreen, "Timer Finished", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@TimerScreen, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.start() //.start() → switches it ON and starts ticking.
                 //keeps a reference so you can pause/cancel it
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201

        timerRunning = true
        pauseButton.text = "Pause"
    }

    private fun pauseTimer() {
<<<<<<< HEAD
        countDownTimer?.cancel()
=======
        countDownTimer?.cancel() //countDownTimer?.cancel() → Stops the timer at its current time (doesn’t reset it).
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
        timerRunning = false
        pauseButton.text = "Resume"
    }

    private fun cancelTimer() {
<<<<<<< HEAD
        isCancelled=true
        saveMeditationSession(totalTimeMillis,"CANCELLED")
        countDownTimer?.cancel()
        timerRunning = false
=======
        countDownTimer?.cancel()  //countDownTimer?.cancel() → Stops the timer completely.
        timerRunning = false
        timeLeftMillis = totalTimeMillis
        updateTimerText()
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
    }

    private fun updateTimerText() {
        val hours = (timeLeftMillis / 1000) / 3600
        val minutes = ((timeLeftMillis / 1000) % 3600) / 60
        val seconds = (timeLeftMillis / 1000) % 60

<<<<<<< HEAD
        timerText.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes,seconds)
=======
        timerText.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
<<<<<<< HEAD

    @SuppressLint("SuspiciousIndentation")
    private fun saveMeditationSession(durationMillis: Long, status:String) {
        val firebaseUser = firebaseAuth.currentUser
        val currentUserId = userManager.getUserId(firebaseUser)

        if (currentUserId.isNullOrEmpty()) {
            Log.e("TimerScreen", "No user ID found, cannot save session")
            Toast.makeText(this, "Please log in to save sessions", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionToSave = UserClass(
            sessionId = 0,
            sessionOwnerId = currentUserId,
            completionTimestamp = System.currentTimeMillis(),
            workDuration = durationMillis,
            status = status
        )

        mUserViewModel.viewModelScope.launch {
            try {
                mUserViewModel.saveNewSession(sessionToSave)
                mUserViewModel.updateAllStatistics(currentUserId, currentUserId)
                broadcastSessionUpdate()

                Log.d("TimerScreen", "Session completed & statistics updated in real-time")
                Log.d("TimerScreen", "User ID: $currentUserId, Duration: $durationMillis, Status: $status")

            } catch (e: Exception) {
                Log.e("TimerScreen", "Error saving session", e)
            }
        }

        Toast.makeText(this, "Session Saved & Statistics Updated", Toast.LENGTH_SHORT).show()
    }

    private fun broadcastSessionUpdate() {
        val intent = Intent("SESSION_UPDATED")
        sendBroadcast(intent)
    }
}
=======
}
>>>>>>> 22c7d16364b6d852c7226ebbefba9be79c3d7201
