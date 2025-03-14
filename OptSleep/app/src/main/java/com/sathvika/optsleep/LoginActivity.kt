@file:Suppress("DEPRECATION")

package com.sathvika.optsleep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.FitnessOptions
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private lateinit var btnFetchData: Button
    private lateinit var googleSignInClient: GoogleSignInClient

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(com.google.android.gms.fitness.data.DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnFetchData = findViewById(R.id.btnFetchData)
        btnFetchData.setOnClickListener {
            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    this,
                    0,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions
                )
            } else {
                fetchSleepData()
            }
        }
    }




    private fun fetchSleepData() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(7)

        val readRequest = com.google.android.gms.fitness.request.DataReadRequest.Builder()
            .read(com.google.android.gms.fitness.data.DataType.TYPE_SLEEP_SEGMENT)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        com.google.android.gms.fitness.Fitness.getHistoryClient(
            this,
            GoogleSignIn.getLastSignedInAccount(this)!!
        )
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

                // Navigate to MainActivity with data
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("totalSleepTime", totalSleepTime)
                intent.putExtra("sleepScore", sleepScore)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch sleep data: ${e.message}")
            }
    }

    private fun calculateSleepDuration(point: com.google.android.gms.fitness.data.DataPoint): Long {
        val start = point.getStartTime(TimeUnit.MILLISECONDS)
        val end = point.getEndTime(TimeUnit.MILLISECONDS)
        return end - start
    }

    private fun countInterruptions(point: com.google.android.gms.fitness.data.DataPoint): Int {
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
