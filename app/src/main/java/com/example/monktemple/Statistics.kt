package com.example.monktemple

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.monktemple.RoomUser.UserClass
import com.example.monktemple.RoomUser.UserViewModel
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import com.example.monktemple.databinding.ActivityStatisticsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class Statistics : AppCompatActivity() {
    private val binding by lazy {
        ActivityStatisticsBinding.inflate(layoutInflater)
    }
    private var activeSessionsLiveData: LiveData<List<UserClass>>? = null
    private lateinit var mUserViewModel: UserViewModel
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentRange = "day"
    private lateinit var sessionManager: SessionManager
    private lateinit var userManager: UserManager
    private lateinit var sessionUpdateReceiver: BroadcastReceiver

    private val grayColor = "#A9A9A9".toColorInt()
    private val chartBarColor = "#95E22A".toColorInt()

    private var currentChartSessions: List<UserClass> =emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mUserViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[UserViewModel::class.java]

        firebaseAuth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)
        userManager = UserManager(sessionManager)


        // When user click on the bar of the day chart Data related to it show functionality
        binding.barChart.setOnChartValueSelectedListener(
            object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(
                    e: com.github.mikephil.charting.data.Entry,
                    h: Highlight
                ) {
               if(currentRange!="day")
                   return
                    val clickedHour=e.x.toInt()
                    val sessionInHour = currentChartSessions.filter{
                        session-> val calendar=Calendar.getInstance()
                        calendar.timeInMillis=session.completionTimestamp
                        calendar.get(Calendar.HOUR_OF_DAY)==clickedHour
                    }
                    if(sessionInHour.isEmpty()) {
                        Log.d("ChartClick","No sessions found for hour: $clickedHour")
                        return
                    }
                    val detailsMessage= StringBuilder()
                    sessionInHour.forEach{ session ->
                        val duration= formatDuration(session.workDuration)
                        val timeFormat=java.text.SimpleDateFormat("h:mm a",Locale.getDefault())
                        val timestamp= timeFormat.format(session.completionTimestamp)
                        detailsMessage.append(". Duration: $duration\n")
                        detailsMessage.append(". Timestamp: $timestamp\n")
                    }

                    androidx.appcompat.app.AlertDialog.Builder(this@Statistics)
                        .setTitle("Session Details for ${clickedHour}:00")
                        .setMessage(detailsMessage.toString().trim())
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                    //
                    binding.barChart.highlightValue(null)// Deselect the bar after showing the dialog
                }

                override fun onNothingSelected() {
                    TODO("Not yet implemented")
                }
            }
        )
        binding.backArrowIcon.setOnClickListener {
            startActivity(Intent(this, Timer::class.java))
            finish()
        }

        binding.dayButton.isSelected = true
        currentRange = "day"

        binding.dayButton.setOnClickListener {
            updateButtonSelection(binding.dayButton)
            currentRange = "day"
            loadDataForCurrentRange()
        }

        binding.weekButton.setOnClickListener {
            updateButtonSelection(binding.weekButton)
            currentRange = "week"
            loadDataForCurrentRange()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("Statistics", "Swipe to refresh triggered.")
            loadDataForCurrentRange()
        }

        setupSessionUpdateReceiver()
        loadDataForCurrentRange()
    }

    //@SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun setupSessionUpdateReceiver() {
        sessionUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "SESSION_UPDATED") {
                    Log.d("Statistics", "Received session update broadcast - refreshing data")
                    loadDataForCurrentRange()
                }
            }
        }

        val filter = IntentFilter("SESSION_UPDATED")
        registerReceiver(sessionUpdateReceiver, filter,Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onResume() {
        super.onResume()
        loadDataForCurrentRange()
    }

    override fun onPause() {
        super.onPause()
        activeSessionsLiveData?.removeObservers(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(sessionUpdateReceiver)
        } catch (e: Exception) {
            Log.e("Statistics", "Error unregistering receiver", e)
        }
    }

    private fun loadDataForCurrentRange() {
        binding.swipeRefreshLayout.isRefreshing = true

        val currentUserId = userManager.getCurrentUserID()

        if (currentUserId.isNullOrEmpty()) {
            Log.d("Statistics", "User not logged in - showing empty state")
            clearChartAndStats()
            binding.swipeRefreshLayout.isRefreshing = false
            ShowToast("Please log in to view statistics")
            return
        }

        val (startDate, endDate) = when (currentRange) {
            "day" -> getTodayRange()
            "week" -> getWeekRange()
            else -> getTodayRange()
        }

        Log.d("Statistics", "Loading REAL-TIME data for user: $currentUserId")

        lifecycleScope.launch {
            try {
                val statistics = mUserViewModel.getStatisticsByFirebaseUid(
                    currentUserId, currentRange, startDate, endDate
                )

                if (statistics != null) {
                    Log.d("Statistics", "Displaying updated statistics")
                    displayStatistics(statistics)
                } else {
                    Log.d("Statistics", "Calculating fresh statistics")
                    mUserViewModel.calculateAndSaveStatistics(
                        currentUserId, currentUserId, currentRange, startDate, endDate
                    )
                    // Don't call loadDataForCurrentRange() recursively, wait for update
                }

                loadSessionsForChart(currentUserId, startDate, endDate)

            } catch (e: Exception) {
                Log.e("Statistics", "Error loading data", e)
                runOnUiThread {
                    binding.swipeRefreshLayout.isRefreshing = false
                    ShowToast("Error loading statistics")
                    clearChartAndStats() // Clear on error
                }
            }
        }
    }

    private fun loadSessionsForChart(userId: String, startDate: Long, endDate: Long) {
        try {
            activeSessionsLiveData?.removeObservers(this@Statistics)

            activeSessionsLiveData = mUserViewModel.getSessionsForDateRange(userId, startDate, endDate)
            activeSessionsLiveData?.observe(this@Statistics) { sessions ->
                this.currentChartSessions=sessions
                runOnUiThread {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Log.d("Statistics", "Chart sessions loaded: ${sessions.size}")

                    try {
                        when (currentRange) {
                            "day" -> updateDayBarChart(sessions)
                            "week" -> updateWeekBarChart(sessions)
                        }
                        calculateAndDisplayStats(sessions)
                    } catch (e: Exception) {
                        Log.e("Statistics", "Error updating chart", e)
                        clearChartAndStats()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Statistics", "Error loading sessions for chart", e)
            runOnUiThread {
                binding.swipeRefreshLayout.isRefreshing = false
                clearChartAndStats()
            }
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    private fun getWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    private fun updateDayBarChart(sessions: List<UserClass>) {
        try {
            val barChart = binding.barChart
            val hourlyData = FloatArray(24) { 0f }
            val calendar = Calendar.getInstance()

            sessions.forEach { session ->
                calendar.timeInMillis = session.completionTimestamp
                val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                if (hourOfDay in hourlyData.indices) {
                    hourlyData[hourOfDay] += (session.workDuration / (1000f * 60f))
                }
            }

            val entries = ArrayList<BarEntry>()
            hourlyData.forEachIndexed { index, minutes ->
                entries.add(BarEntry(index.toFloat(), minutes))
            }
            val labels = Array(24) { i -> if (i % 4 == 0) "$i:00" else "" }
            updateChart(barChart, entries, "Time per Hour (min)", labels)
        } catch (e: Exception) {
            Log.e("Statistics", "Error in updateDayBarChart", e)
            clearChartAndStats()
        }
    }

    private fun updateWeekBarChart(sessions: List<UserClass>) {
        try {
            val barChart = binding.barChart
            val dailyData = FloatArray(7) { 0f }
            val calendar = Calendar.getInstance()

            sessions.forEach { session ->
                calendar.timeInMillis = session.completionTimestamp
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                if (dayIndex in dailyData.indices) {
                    dailyData[dayIndex] += (session.workDuration / (1000f * 60f))
                }
            }

            val entries = ArrayList<BarEntry>()
            val labels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            dailyData.forEachIndexed { index, minutes ->
                entries.add(BarEntry(index.toFloat(), minutes))
            }
            updateChart(barChart, entries, "Time per Day (min)", labels)
        } catch (e: Exception) {
            Log.e("Statistics", "Error in updateWeekBarChart", e)
            clearChartAndStats()
        }
    }

    private fun updateChart(
        barChart: BarChart,
        entries: List<BarEntry>,
        label: String,
        axisLabels: Array<String>
    ) {
        try {
            val barDataSet = BarDataSet(entries, label)
            barDataSet.color = chartBarColor
            barDataSet.valueTextColor = Color.WHITE
            barDataSet.setDrawValues(false)

            val barData = BarData(barDataSet)
            barChart.data = barData
            barChart.setFitBars(true)

            styleChart(barChart, axisLabels)
            barChart.invalidate()
        } catch (e: Exception) {
            Log.e("Statistics", "Error in updateChart", e)
        }
    }

    private fun styleChart(barChart: BarChart, axisLabels: Array<String>) {
        try {
            barChart.description.isEnabled = false
            barChart.legend.isEnabled = false
            barChart.setDrawGridBackground(false)
            barChart.setDrawBorders(false)
            barChart.setTouchEnabled(true)
            barChart.animateY(800)

            val xAxis = barChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.textColor = grayColor
            xAxis.axisLineColor = grayColor
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(axisLabels)

            val yAxis = barChart.axisLeft
            yAxis.textColor = grayColor
            yAxis.axisLineColor = grayColor
            yAxis.gridColor = grayColor
            yAxis.setDrawGridLines(true)
            yAxis.axisMinimum = 0f

            val data = barChart.data
            val maxValue = data?.yMax ?: 180f
            yAxis.axisMaximum = if (maxValue > 10f) maxValue * 1.2f else 30f
            yAxis.granularity = if (maxValue > 120f) 60f else 30f

            barChart.axisRight.isEnabled = false
        } catch (e: Exception) {
            Log.e("Statistics", "Error in styleChart", e)
        }
    }

    private fun updateButtonSelection(selectedButton: Button) {
        binding.dayButton.isSelected = false
        binding.weekButton.isSelected = false
        selectedButton.isSelected = true
    }

    private fun calculateAndDisplayStats(sessions: List<UserClass>) {
        try {
            val totalAttempts = sessions.size
            val completedSessions = sessions.filter { it.status == "COMPLETED" }

            val totalFocusMillis = completedSessions.sumOf { it.workDuration }
            val averageSessionMillis =
                if (completedSessions.isNotEmpty()) totalFocusMillis / completedSessions.size else 0L

            binding.noOfSession.text = completedSessions.size.toString()
            binding.focusTIme.text = formatDuration(totalFocusMillis)
            binding.sessionTime.text = formatDuration(averageSessionMillis)

            val longestSession = completedSessions.maxByOrNull { it.workDuration }
            binding.longestSessionValue.text =
                longestSession?.let { formatDuration(it.workDuration) } ?: "0m"

            val completionRate = if (totalAttempts > 0) {
                (completedSessions.size.toDouble() / totalAttempts.toDouble() * 100)
            } else {
                0.0
            }
            binding.completionRateValue.text =
                String.format(Locale.US, "%.0f%%", completionRate)

            binding.mostProductiveTime.text = calculateMostProductiveDay(completedSessions)
        } catch (e: Exception) {
            Log.e("Statistics", "Error in calculateAndDisplayStats", e)
        }
    }

    private fun displayStatistics(statistics: com.example.monktemple.RoomUser.NewStatistics) {
        try {
            binding.noOfSession.text = statistics.noOfSessions.toString()
            binding.focusTIme.text = formatDuration(statistics.focusTime)
            binding.sessionTime.text = formatDuration(statistics.averageSessionTime)
            binding.longestSessionValue.text = formatDuration(statistics.longestSession)
            binding.completionRateValue.text = String.format(Locale.US, "%.0f%%", statistics.completionRate)
            binding.mostProductiveTime.text = statistics.mostProductiveDay
        } catch (e: Exception) {
            Log.e("Statistics", "Error in displayStatistics", e)
        }
    }

    private fun calculateMostProductiveDay(sessions: List<UserClass>): String {
        return try {
            if (sessions.isEmpty()) return "N/A"
            val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            sessions
                .groupBy {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = it.completionTimestamp
                    days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
                }
                .mapValues { entry -> entry.value.sumOf { it.workDuration } }
                .maxByOrNull { it.value }?.key ?: "N/A"
        } catch (e: Exception) {
            Log.e("Statistics", "Error in calculateMostProductiveDay", e)
            "N/A"
        }
    }

    private fun formatDuration(millis: Long): String {
        return try {
            if (millis <= 0) return "0m"
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        } catch (e: Exception) {
            Log.e("Statistics", "Error in formatDuration", e)
            "0m"
        }
    }

    private fun clearChartAndStats() {
        try {
            calculateAndDisplayStats(emptyList())
            binding.barChart.clear()
            binding.barChart.invalidate()
        } catch (e: Exception) {
            Log.e("Statistics", "Error in clearChartAndStats", e)
        }
    }
}