package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import nl.joery.timerangepicker.TimeRangePicker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Declaring buttons
        val startbutton = findViewById<Button>(R.id.startButton)
        val timePicker = findViewById<TimeRangePicker>(R.id.picker)

        startbutton.setOnClickListener {
            val intent = Intent(this, TimerScreen::class.java)
            val duration = timePicker.duration
            val time = duration.durationMinutes

            intent.putExtra("time", time)
            startActivity(intent)
            //Pass the data to the next activity



        }

        timePicker.setOnDragChangeListener(object : TimeRangePicker.OnDragChangeListener {
            override fun onDragStart(thumb: TimeRangePicker.Thumb): Boolean {
                // Allow dragging
               // return thumb != TimeRangePicker.Thumb.START // This allow dragging  of ThumbEnd Icon
                    // but don't allow the dragging of ThumbStart Icon
           return true
            }

            override fun onDragStop(thumb: TimeRangePicker.Thumb) {
                if (thumb == TimeRangePicker.Thumb.START) {
                    val startTime = timePicker.startTime
                    Toast.makeText(
                        this@MainActivity,
                        "Start Time: ${startTime.hour}:${startTime.minute}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (thumb == TimeRangePicker.Thumb.END) {
                    val endTime = timePicker.endTime
                    Toast.makeText(
                        this@MainActivity,
                        "End Time: ${endTime.hour}:${endTime.minute}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

    }
}