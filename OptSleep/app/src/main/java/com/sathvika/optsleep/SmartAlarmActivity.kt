package com.sathvika.optsleep

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

@Suppress("DEPRECATION")
class SmartAlarmActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_smart_alarm)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val recommendedHour = (currentHour + 8) % 24
        val recommendedAmPm = if (recommendedHour < 12) "AM" else "PM"
        val displayHour = if (recommendedHour == 0 || recommendedHour == 12) 12 else recommendedHour % 12


        val recommendationText: TextView = findViewById(R.id.recommendationText)
        recommendationText.text = "The recommended time to set your alarm is $displayHour:00 $recommendedAmPm."

        val setAlarmButton: Button = findViewById(R.id.setAlarmButton)
        setAlarmButton.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val amPm = if (hour < 12) "AM" else "PM"
                    val alarmHour = if (hour == 0 || hour == 12) 12 else hour % 12
                    Toast.makeText(
                        this,
                        "Alarm set for $alarmHour:${minute.toString().padStart(2, '0')} $amPm",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                currentHour,
                currentMinute,
                false // Use 12-hour format
            ).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return true
    }
}
