package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class TimerScreen : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var pauseButton: Button
    private lateinit var cancelButton: Button

    private var countDownTimer: CountDownTimer? = null
    private var totalTimeMillis: Long = 0L
    private var timeLeftMillis: Long = 0L
    private var timerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timer_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind UI
        timerText = findViewById(R.id.textView)
        pauseButton = findViewById(R.id.button)
        cancelButton = findViewById(R.id.button2)

        // Get duration (in minutes) from Intent and convert to milliseconds
        val durationInMinutes = intent.getIntExtra("time", 0)
        totalTimeMillis = durationInMinutes.toLong() * 60 * 1000L
        timeLeftMillis = totalTimeMillis

        updateTimerText()

        // Start timer immediately
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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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

        timerRunning = true
        pauseButton.text = "Pause"
    }

    private fun pauseTimer() {
        countDownTimer?.cancel() //countDownTimer?.cancel() → Stops the timer at its current time (doesn’t reset it).
        timerRunning = false
        pauseButton.text = "Resume"
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()  //countDownTimer?.cancel() → Stops the timer completely.
        timerRunning = false
        timeLeftMillis = totalTimeMillis
        updateTimerText()
    }

    private fun updateTimerText() {
        val hours = (timeLeftMillis / 1000) / 3600
        val minutes = ((timeLeftMillis / 1000) % 3600) / 60
        val seconds = (timeLeftMillis / 1000) % 60

        timerText.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
