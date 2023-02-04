package com.yandex.maps.testapp.search

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.jjoe64.graphview.GraphView
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.Session
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.search.test.generateTestCaseList
import com.yandex.maps.testapp.search.test.makeAverageSeries
import com.yandex.maps.testapp.search.test.makeConstantValueSeries
import com.yandex.maps.testapp.search.test.makeTestResultSeries
import com.yandex.runtime.Error
import kotlin.math.max

class PerformanceTestsActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    private val testResults by lazy { find<TextView>(R.id.performance_test_result_text) }
    private val startTestsButton by lazy { find<Button>(R.id.start_performance_test_button) }
    private val progressBar by lazy { find<ProgressBar>(R.id.performance_tests_progress_bar) }
    private val graph by lazy { find<GraphView>(R.id.graph_view) }

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.OFFLINE)
    private var searchSession: Session? = null
    private val regionDownloadHelper = RegionDownloadHelper()

    private var testCaseList = listOf<PerformanceTestCase>()
    private var testCaseIndex: Int = 0
    private val seriesColors = arrayOf(Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN)
    private var seriesColorIndex: Int = 0

    private val searchListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            testCase().finish = System.currentTimeMillis()
            progressBar.progress = ++testCaseIndex
            if (testCaseIndex < testCaseList.size) {
                runNextTest()
            } else {
                message("Test run finished", Color.YELLOW)
                showUsageStats()
                startTestsButton.isEnabled = true
            }
        }
        override fun onSearchError(error: Error) {
            message("Broken test ${testCase()}, no errors expected", Color.RED)
            startTestsButton.isEnabled = true
        }
    }

    private fun showUsageStats() {
        testCaseList
            .filter { it.shouldAddToStats }
            .groupBy { it.region }
            .forEach { group ->
                val color = nextSeriesColor()
                graph.addSeries(makeTestResultSeries(group.value, color))
                graph.addSeries(makeAverageSeries(group.value, color))
                message(
                    buildUsageString(
                        group.value.map { it.duration() },
                        group.key
                    ),
                    Color.DKGRAY
                )
            }

        val totalDuration = testCaseList.sumBy { it.duration().toInt() }
        val averageDuration = totalDuration.toDouble() / testCaseList.size
        message(
            "Test case average duration: ${averageDuration.toInt()}",
            Color.DKGRAY
        )

        updateGraphViewport()
    }

    private fun nextSeriesColor() = seriesColors[seriesColorIndex++ % seriesColors.size]

    private fun buildUsageString(stats: List<Long>, usageTag: String): String {
        return "$usageTag: [${stats.joinToString()}], " +
            "min=${stats.minOrNull()}, " +
            "max=${stats.maxOrNull()}, " +
            "average=${stats.average()}"
    }

    private fun runNextTest() {
        if (testCaseIndex >= testCaseList.size) { return; }

        testCase().start = System.currentTimeMillis()
        searchSession = if (testCase().isReverseSearchQuery()) {
            searchManager.submit(testCase().point.point!!,
                null,
                makeSearchOptions(testCase().searchTypes, null),
                searchListener
            )
        } else {
            searchManager.submit(testCase().query,
                testCase().point,
                makeSearchOptions(testCase().searchTypes, null),
                searchListener
            )
        }
    }

    private fun testCase() = testCaseList[testCaseIndex]

    @Suppress("UNUSED_PARAMETER")
    fun runTests(view: View) {
        startTestsButton.isEnabled = false
        testCaseList = generateTestCaseList()
        testCaseIndex = 0
        progressBar.max = testCaseList.size
        progressBar.progress = testCaseIndex
        regionDownloadHelper.download()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.search_performance_tests)
        notifyOnDeviceKind()
        setupGraph()

        regionDownloadHelper.setup(hashMapOf(
            1 to find(R.id.msk_progress_bar),
            10174 to find(R.id.spb_progress_bar)
        ))
        regionDownloadHelper.onDownloadStarted = {
            findViewById<View>(R.id.download_data_layout).show()
        }
        regionDownloadHelper.onAllRegionsReady = {
            findViewById<View>(R.id.download_data_layout).hide()
            message("All regions are ready", Color.GRAY)
            // We need to provide timeout for lazy offline search loading
            Handler().postDelayed({ runNextTest() }, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchSession?.cancel()
        regionDownloadHelper.onDownloadStarted = {}
        regionDownloadHelper.onAllRegionsReady = {}
    }

    private fun notifyOnDeviceKind() {
        message("Low-end device, threshold 2000 ms", Color.RED)
        message("Middle-end device, threshold 1000 ms", Color.YELLOW)
        message("High-end device, threshold 400 ms", Color.GREEN)
    }

    private fun setupGraph() {
        graph.title = getString(R.string.test_run_vs_response_time)
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMinX(1.0)
        graph.viewport.setMaxX(11.0)
        graph.viewport.setMinY(0.0)
        graph.viewport.setMaxY(2000.0)

        graph.addSeries(makeConstantValueSeries(2000.0 /* ms */, -1, 2000, Color.RED))
        graph.addSeries(makeConstantValueSeries(1000.0 /* ms */, -1, 2000, Color.YELLOW))
        graph.addSeries(makeConstantValueSeries(400.0 /* ms */, -1, 2000, Color.GREEN))
    }

    private fun updateGraphViewport() {
        if (graph.series.isEmpty()) { return; }

        val yPadding = 50
        graph.viewport.setMinY(
            max(
                graph.series.map { it.lowestValueY }.minOrNull()!! - yPadding,
                0.0
            )
        )
        graph.viewport.setMaxY(
            graph.series.map { it.highestValueY }.maxOrNull()!! + yPadding
        )

        fun seriesWithoutThreshold() = graph.series.drop(3)
        val xPadding = 0.01
        graph.viewport.setMinX(
            max(
                seriesWithoutThreshold().map { it.lowestValueX }.minOrNull()!! - xPadding,
                0.0
            )
        )
        graph.viewport.setMaxX(
            seriesWithoutThreshold().map { it.highestValueX }.maxOrNull()!! + xPadding
        )
    }

    private fun message(text: String, color: Int) {
        val stringBuilder = SpannableStringBuilder(text + '\n')
        stringBuilder.setSpan(ForegroundColorSpan(color),
            0,
            stringBuilder.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        testResults.append(stringBuilder)
    }

}
