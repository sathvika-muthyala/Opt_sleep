@file:Suppress("DEPRECATION")

package com.sathvika.optsleep

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var tvSleepData: TextView
    private lateinit var tvSleepScore: TextView
    private lateinit var tvRecommendations: TextView
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnRefreshData: Button
    private lateinit var btnSmartAlarm: Button
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(com.google.android.gms.fitness.data.DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        tvSleepData = findViewById(R.id.tvSleepData)
        tvSleepScore = findViewById(R.id.tvSleepScore)
        tvRecommendations = findViewById(R.id.tvRecommendations)
        btnRefreshData = findViewById(R.id.btnRefreshData)
        btnSmartAlarm = findViewById(R.id.btnSmartAlarm)
        btnSmartAlarm.setOnClickListener {
            val intent = Intent(this, SmartAlarmActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP // To bring the existing instance to the front if it's already running
            startActivity(intent)
        }

        val totalSleepTime = intent.getLongExtra("totalSleepTime", 0L)
        val sleepScore = intent.getIntExtra("sleepScore", 0)

        updateUI(totalSleepTime, sleepScore)
        btnRefreshData.setOnClickListener {
            fetchSleepData() // Fetch sleep data again without re-login
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("MainActivity", "Logged out successfully")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(totalSleepTime: Long, sleepScore: Int) {
        val hours = TimeUnit.MILLISECONDS.toHours(totalSleepTime)
        tvSleepData.text = "Total Sleep: $hours hours"
        tvSleepScore.text = "Sleep Score: $sleepScore"
        tvRecommendations.text = when {
            sleepScore >= 90 -> "Great job! Keep maintaining your sleep schedule."
            sleepScore in 70..89 -> "Good work, but try to minimize interruptions or increase sleep duration."
            else -> "Consider going to bed earlier and reducing screen time before sleep."
        }
    }
    private fun fetchSleepData() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null || !GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            Log.e(TAG, "User is not signed in or permissions not granted")
            return
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(7)

        val readRequest = DataReadRequest.Builder()
            .read(com.google.android.gms.fitness.data.DataType.TYPE_SLEEP_SEGMENT)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        com.google.android.gms.fitness.Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                var totalSleepTime = 0L
                var interruptions = 0

                for (dataSet in response.dataSets) {
                    for (point in dataSet.dataPoints) {
                        totalSleepTime += calculateSleepDuration(point)
                        interruptions += countInterruptions(point)
                    }
                }

                val sleepScore = calculateSleepScore(totalSleepTime, interruptions)

                // Update UI with the new data
                updateUI(totalSleepTime, sleepScore)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch sleep data: ${e.message}")
            }
    }

    private fun calculateSleepDuration(point: DataPoint): Long {
        val start = point.getStartTime(TimeUnit.MILLISECONDS)
        val end = point.getEndTime(TimeUnit.MILLISECONDS)
        return end - start
    }

    private fun countInterruptions(point: DataPoint): Int {
        return point.dataType.fields.size // Example logic for counting interruptions
    }

    private fun calculateSleepScore(totalSleepTime: Long, interruptions: Int): Int {
        val hours = TimeUnit.MILLISECONDS.toHours(totalSleepTime)
        val idealSleepHours = 8
        val maxInterruptions = 3

        val durationScore = max(0, min(100, (hours / idealSleepHours.toDouble() * 100).toInt()))
        val interruptionScore = max(0, 100 - (interruptions * (100 / maxInterruptions)))
        return (0.7 * durationScore + 0.3 * interruptionScore).toInt()
    }
}

