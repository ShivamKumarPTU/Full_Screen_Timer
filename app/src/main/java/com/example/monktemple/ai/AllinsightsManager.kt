package com.example.monktemple.ai

import android.content.Context
import android.util.Log
import com.example.monktemple.RoomUser.UserClass
import com.example.monktemple.RoomUser.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIInsightsManager @Inject constructor(
    private val context: Context,
    private val userDao: UserDao
) {

    companion object {
        private const val TAG = "AIInsightsManager"
    }

    data class ProductivityInsights(
        val optimalSessionLength: Long,
        val peakProductivityHours: List<Int>,
        val weeklyTrend: String,
        val recommendations: List<String>,
        val focusScore: Int,
        val consistencyScore: Double
    )

    suspend fun generateInsights(userId: String): ProductivityInsights {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🤖 Generating AI insights for user: $userId")

                // Get last 30 days of sessions
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val startDate = calendar.timeInMillis
                val endDate = System.currentTimeMillis()

                val sessions = userDao.getSessionsForUserInRangeImmediate(userId, startDate, endDate)
                val completedSessions = sessions.filter { it.status == "COMPLETED" }

                if (completedSessions.isEmpty()) {
                    return@withContext getDefaultInsights()
                }

                val optimalLength = calculateOptimalSessionLength(completedSessions)
                val peakHours = calculatePeakProductivityHours(completedSessions)
                val weeklyTrend = analyzeWeeklyTrend(completedSessions)
                val focusScore = calculateFocusScore(completedSessions)
                val consistencyScore = calculateConsistencyScore(completedSessions)
                val recommendations = generateRecommendations(
                    completedSessions, optimalLength, peakHours, weeklyTrend, focusScore, consistencyScore
                )

                ProductivityInsights(
                    optimalSessionLength = optimalLength,
                    peakProductivityHours = peakHours,
                    weeklyTrend = weeklyTrend,
                    recommendations = recommendations,
                    focusScore = focusScore,
                    consistencyScore = consistencyScore
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error generating insights", e)
                getDefaultInsights()
            }
        }
    }

    private fun calculateOptimalSessionLength(sessions: List<UserClass>): Long {
        val completedSessions = sessions.filter { it.status == "COMPLETED" }
        if (completedSessions.isEmpty()) return 25L

        val successfulSessions = completedSessions.filter { it.workDuration > 300000 } // 5+ minutes
        if (successfulSessions.isEmpty()) return 25L

        val averageDuration = successfulSessions.map { it.workDuration / (1000 * 60) }.average()
        return (averageDuration / 5).toInt() * 5L // Round to nearest 5 minutes
    }

    private fun calculatePeakProductivityHours(sessions: List<UserClass>): List<Int> {
        val calendar = Calendar.getInstance()
        val hourProductivity = mutableMapOf<Int, Double>()

        // Initialize all hours
        for (hour in 0..23) {
            hourProductivity[hour] = 0.0
        }

        sessions.forEach { session ->
            calendar.timeInMillis = session.completionTimestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val productivity = session.workDuration.toDouble() / (1000 * 60) // minutes
            hourProductivity[hour] = hourProductivity.getOrDefault(hour, 0.0) + productivity
        }

        // Return top 3 most productive hours
        return hourProductivity.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
            .sorted()
    }

    private fun analyzeWeeklyTrend(sessions: List<UserClass>): String {
        if (sessions.size < 14) return "insufficient_data"

        val calendar = Calendar.getInstance()
        val lastWeekEnd = System.currentTimeMillis()
        val lastWeekStart = lastWeekEnd - (7 * 24 * 60 * 60 * 1000)
        val previousWeekStart = lastWeekStart - (7 * 24 * 60 * 60 * 1000)
        val previousWeekEnd = lastWeekStart - 1

        val lastWeekSessions = sessions.filter { it.completionTimestamp in lastWeekStart..lastWeekEnd }
        val previousWeekSessions = sessions.filter { it.completionTimestamp in previousWeekStart..previousWeekEnd }

        val lastWeekFocus = lastWeekSessions.sumOf { it.workDuration }
        val previousWeekFocus = previousWeekSessions.sumOf { it.workDuration }

        return when {
            previousWeekFocus == 0L -> "improving"
            lastWeekFocus > previousWeekFocus * 1.2 -> "improving_rapidly"
            lastWeekFocus > previousWeekFocus * 1.1 -> "improving"
            lastWeekFocus < previousWeekFocus * 0.8 -> "declining"
            else -> "stable"
        }
    }

    private fun calculateFocusScore(sessions: List<UserClass>): Int {
        val completedSessions = sessions.filter { it.status == "COMPLETED" }
        val totalSessions = sessions.size

        if (totalSessions == 0) return 0

        val completionRate = completedSessions.size.toDouble() / totalSessions.toDouble()
        val averageDuration = if (completedSessions.isNotEmpty()) {
            completedSessions.map { it.workDuration }.average() / (1000 * 60) // minutes
        } else 0.0

        val consistency = calculateConsistencyScore(sessions)

        // Weighted scoring: completion(40%), duration(30%), consistency(30%)
        val score = (completionRate * 40) +
                ((averageDuration / 60).coerceIn(0.0, 1.0) * 30) +
                (consistency * 30)

        return score.toInt().coerceIn(0, 100)
    }

    private fun calculateConsistencyScore(sessions: List<UserClass>): Double {
        if (sessions.size < 7) return 0.5

        val calendar = Calendar.getInstance()
        val dailyFocus = mutableMapOf<Int, Long>()

        sessions.forEach { session ->
            calendar.timeInMillis = session.completionTimestamp
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            dailyFocus[dayOfYear] = dailyFocus.getOrDefault(dayOfYear, 0L) + session.workDuration
        }

        if (dailyFocus.size < 3) return 0.3

        val averageFocus = dailyFocus.values.average()
        if (averageFocus == 0.0) return 0.0

        val variance = dailyFocus.values.map {
            Math.pow((it - averageFocus) / averageFocus, 2.0)
        }.average()

        return (1.0 - variance.coerceIn(0.0, 1.0))
    }

    private fun generateRecommendations(
        sessions: List<UserClass>,
        optimalLength: Long,
        peakHours: List<Int>,
        trend: String,
        focusScore: Int,
        consistencyScore: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Session length recommendations
        when {
            optimalLength < 15 -> recommendations.add("Try gradually increasing session length to build focus stamina")
            optimalLength > 90 -> recommendations.add("Consider breaking long sessions into focused chunks with breaks")
            else -> recommendations.add("Your ${optimalLength}-minute sessions are working well - maintain this duration")
        }

        // Time recommendations
        if (peakHours.isNotEmpty()) {
            val bestTime = when (peakHours.first()) {
                in 5..10 -> "morning"
                in 11..15 -> "midday"
                in 16..20 -> "afternoon"
                else -> "evening"
            }
            recommendations.add("You're most productive in the $bestTime. Schedule important work then.")
        }

        // Trend-based recommendations
        when (trend) {
            "improving_rapidly" -> recommendations.add("Excellent progress! Consider setting more ambitious goals")
            "improving" -> recommendations.add("You're building great momentum - keep your current routine")
            "declining" -> recommendations.add("Try reducing session length to rebuild consistency")
            "stable" -> recommendations.add("Consistency is key. Try adding one extra session per week")
        }

        // Focus score recommendations
        when {
            focusScore < 30 -> recommendations.add("Start with shorter, more frequent sessions to build habit")
            focusScore > 80 -> recommendations.add("You're a focus master! Challenge yourself with complex tasks")
            else -> recommendations.add("Good foundation. Try eliminating one distraction at a time")
        }

        // Consistency recommendations
        when {
            consistencyScore < 0.3 -> recommendations.add("Focus on building daily habit rather than long sessions")
            consistencyScore > 0.8 -> recommendations.add("Great consistency! Your routine is working well")
        }

        return recommendations.distinct().take(4) // Limit to 4 unique recommendations
    }

    private fun getDefaultInsights(): ProductivityInsights {
        return ProductivityInsights(
            optimalSessionLength = 25L,
            peakProductivityHours = listOf(9, 14, 16),
            weeklyTrend = "stable",
            recommendations = listOf(
                "Start with 25-minute focus sessions (Pomodoro technique)",
                "Take 5-minute breaks between sessions to maintain focus",
                "Schedule sessions during your most alert hours of the day"
            ),
            focusScore = 50,
            consistencyScore = 0.5
        )
    }

    suspend fun getOptimalSessionTime(userId: String): String {
        val insights = generateInsights(userId)
        return if (insights.peakProductivityHours.isNotEmpty()) {
            val bestHour = insights.peakProductivityHours.first()
            "${bestHour}:00"
        } else {
            "14:00" // Default 2 PM
        }
    }
}