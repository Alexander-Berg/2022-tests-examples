package com.yandex.vanga.app.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yandex.vanga.Vanga
import com.yandex.vanga.app.R

class VangaTestScoreActivity : AppCompatActivity() {

    private val vanga = Vanga()

    private lateinit var buttonCalculate: Button
    private lateinit var personal: EditText
    private lateinit var hourly: EditText
    private lateinit var weekly: EditText
    private lateinit var recent: EditText
    private lateinit var total: EditText
    private lateinit var totalHourly: EditText
    private lateinit var totalWeekly: EditText
    private lateinit var timeSinceInstall: EditText
    private lateinit var result: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vanga_test)
        title = "Vanga score computation"

        buttonCalculate = findViewById(R.id.btn_calculate)
        personal = findViewById(R.id.personal)
        hourly = findViewById(R.id.hourly)
        weekly = findViewById(R.id.weekly)
        recent = findViewById(R.id.recent)
        total = findViewById(R.id.total)
        totalHourly = findViewById(R.id.total_hourly)
        totalWeekly = findViewById(R.id.total_weekly)
        timeSinceInstall = findViewById(R.id.time_since_install)
        result = findViewById(R.id.result)

        buttonCalculate.setOnClickListener {
            try {
                val personalCount = personal.toFloat()
                val personalHourly = hourly.toFloat()
                val personalWeekly = weekly.toFloat()
                val recentFeature = recent.toFloat()

                val totalCount = total.toFloat()
                val totalHourly = totalHourly.toFloat()
                val totalWeekly = totalWeekly.toFloat()

                val timeSinceInstall = timeSinceInstall.toFloat()

                val score = vanga.getScore(
                        floatArrayOf(personalCount / personalCount,
                                personalHourly / personalCount,
                                personalWeekly / personalCount,
                                recentFeature,
                                totalCount,
                                totalHourly,
                                totalWeekly,
                                timeSinceInstall))

                result.text = "Score: $score"
            } catch (e: NumberFormatException) {
                Toast.makeText(applicationContext, "Only digits are allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }
}

private fun TextView.toFloat() = text.toString().toFloat()
