package com.example.monktemple

import com.example.monktemple.RoomUser.UserViewModel
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

    private var countDownTimer: CountDownTimer? = null
    private var totalTimeMillis: Long = 0L
    private var timeLeftMillis: Long = 0L
    private var timerRunning = false

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        val durationInMinutes = intent.getIntExtra("time", 0)
        totalTimeMillis = durationInMinutes.toLong() * 60 * 1000L
        timeLeftMillis = totalTimeMillis

        updateTimerText()
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

        timerRunning = true
        pauseButton.text = "Pause"
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        pauseButton.text = "Resume"
    }

    private fun cancelTimer() {
        isCancelled=true
        saveMeditationSession(totalTimeMillis,"CANCELLED")
        countDownTimer?.cancel()
        timerRunning = false
    }

    private fun updateTimerText() {
        val hours = (timeLeftMillis / 1000) / 3600
        val minutes = ((timeLeftMillis / 1000) % 3600) / 60
        val seconds = (timeLeftMillis / 1000) % 60

        timerText.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes,seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

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