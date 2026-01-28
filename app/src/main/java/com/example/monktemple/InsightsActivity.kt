package com.example.monktemple

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.monktemple.ai.AIInsightsManager
import com.example.monktemple.databinding.ActivityInsightsBinding
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InsightsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInsightsBinding

    @Inject
    lateinit var insightsManager: AIInsightsManager

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var backButton: Button
    private lateinit var optimalSessionText: TextView
    private lateinit var focusScoreText: TextView
    private lateinit var weeklyTrendText: TextView
    private lateinit var peakHoursText: TextView
    private lateinit var recommendationsText: TextView
    private lateinit var refreshButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()
        loadInsights()
    }

    private fun initializeViews() {
        backButton = binding.backButton
        optimalSessionText = binding.optimalSessionLength
        focusScoreText = binding.focusScore
        weeklyTrendText = binding.weeklyTrend
        peakHoursText = binding.peakHoursText
        recommendationsText = binding.recommendationsText
        refreshButton = binding.refreshInsights
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        refreshButton.setOnClickListener {
            loadInsights()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadInsights() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                refreshButton.isEnabled = false

                val userId = sessionManager.getFirebaseUid()
                if (userId.isNullOrEmpty()) {
                    ShowToast("Please log in to view insights")
                    finish()
                    return@launch
                }

                val insights = insightsManager.generateInsights(userId)

                // Update UI with insights
                optimalSessionText.text = "${insights.optimalSessionLength} min"
                focusScoreText.text = "${insights.focusScore}/100"
                weeklyTrendText.text = formatTrendText(insights.weeklyTrend)
                peakHoursText.text = formatPeakHours(insights.peakProductivityHours)
                recommendationsText.text = formatRecommendations(insights.recommendations)

                // Update progress bars
                updateProgressBars(insights)

                ShowToast("Insights updated!")

            } catch (e: Exception) {
                ShowToast("Error loading insights")
                e.printStackTrace()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                refreshButton.isEnabled = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgressBars(insights: AIInsightsManager.ProductivityInsights) {
        // Focus Score
        binding.focusScoreProgress.progress = insights.focusScore
        binding.focusScoreProgress.max = 100

        // Consistency Score
        val consistencyPercent = (insights.consistencyScore * 100).toInt()
        binding.consistencyProgress.progress = consistencyPercent
        binding.consistencyProgress.max = 100
        binding.consistencyText.text = "$consistencyPercent%"

        // Optimal session indicator
        val optimalProgress = (insights.optimalSessionLength.coerceIn(5, 120) / 120.0 * 100).toInt()
        binding.optimalSessionProgress.progress = optimalProgress
        binding.optimalSessionProgress.max = 100
    }

    private fun formatTrendText(trend: String): String {
        return when (trend) {
            "improving_rapidly" -> "Rapidly Improving 📈"
            "improving" -> "Improving ↗️"
            "declining" -> "Needs Attention 📉"
            "stable" -> "Stable →"
            "insufficient_data" -> "More Data Needed 📊"
            else -> "Analyzing..."
        }
    }

    private fun formatPeakHours(hours: List<Int>): String {
        return if (hours.isNotEmpty()) {
            hours.joinToString(", ") { hour ->
                val period = when (hour) {
                    in 5..11 -> "${hour}AM"
                    in 12..16 -> "${if (hour > 12) hour - 12 else hour}PM"
                    else -> "${if (hour > 12) hour - 12 else hour}PM"
                }
                period
            }
        } else {
            "Not enough data"
        }
    }

    private fun formatRecommendations(recommendations: List<String>): String {
        return if (recommendations.isNotEmpty()) {
            recommendations.joinToString("\n\n") { "• $it" }
        } else {
            "• Continue your current routine\n• Stay consistent with your sessions\n• Track your progress weekly"
        }
    }

    override fun onResume() {
        super.onResume()
        loadInsights()
    }
}