package com.yandex.maps.testapp.search

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.search.test.SearchTestSuite
import com.yandex.maps.testapp.search.test.TestOutput
import com.yandex.maps.testapp.search.test.TestRunner
import com.yandex.runtime.i18n.I18nManagerFactory


class AutotestActivity : TestAppActivity() {
    override fun onStopImpl() {}
    override fun onStartImpl() {}

    private val startOnlineTestButton by lazy { find<Button>(R.id.start_online_test_button) }
    private val startOfflineTestButton by lazy { find<Button>(R.id.start_offline_test_button) }
    private val regionDownloadHelper = RegionDownloadHelper()
    private val suite = SearchTestSuite()

    private var isRussianLocale: Boolean = false

    private val testOutput = object : TestOutput {
        private val testResults by lazy { find<TextView>(R.id.test_result_text) }
        private val testResultsScroll by lazy { find<ScrollView>(R.id.test_result_scroller) }

        override fun message(text: String, color: Int) {
            val stringBuilder = SpannableStringBuilder(text + '\n')
            stringBuilder.setSpan(ForegroundColorSpan(color),
                    0,
                    stringBuilder.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            testResults.append(stringBuilder)
            testResultsScroll.postDelayed({ testResultsScroll.fullScroll(ScrollView.FOCUS_DOWN) }, 100)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun runOnlineTests(view: View) {
        startOnlineTestButton.isEnabled = false
        startOfflineTestButton.isEnabled = false
        runTests(isOnline = true)
    }

    @Suppress("UNUSED_PARAMETER")
    fun runOfflineTests(view: View) {
        startOnlineTestButton.isEnabled = false
        startOfflineTestButton.isEnabled = false
        regionDownloadHelper.download()
    }

    private fun runTests(isOnline: Boolean) {
        val runner = TestRunner(testOutput)
        suite.setUp(runner, isOnline, isRussianLocale)
        runner.onFinished = {
            startOnlineTestButton.isEnabled = true
            startOfflineTestButton.isEnabled = true
        }
        if (isOnline) {
            testOutput.message("Will run ONLINE tests", Color.YELLOW)
            runner.start()
        } else {
            testOutput.message("Will run OFFLINE tests", Color.CYAN)
            // We need to provide timeout for lazy offline search loading
            Handler().postDelayed({ runner.start() }, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_autotest)
        isRussianLocale = (I18nManagerFactory.getLocale() == "ru_RU")

        regionDownloadHelper.setup(
            hashMapOf(
                1 to find(R.id.msk_progress_bar),
                10174 to find(R.id.spb_progress_bar),
                143 to find(R.id.kiev_progress_bar)
            )
        )
        regionDownloadHelper.onDownloadStarted = {
            findViewById<View>(R.id.download_data_layout).show()
        }
        regionDownloadHelper.onAllRegionsReady = {
            findViewById<View>(R.id.download_data_layout).hide()
            testOutput.message("All regions are ready", Color.GRAY)
            runTests(isOnline = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        regionDownloadHelper.onDownloadStarted = {}
        regionDownloadHelper.onAllRegionsReady = {}
    }
}
