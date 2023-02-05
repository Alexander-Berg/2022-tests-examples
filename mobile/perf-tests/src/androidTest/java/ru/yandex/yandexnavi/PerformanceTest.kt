package ru.yandex.yandexnavi

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.Test
import org.junit.runner.RunWith

const val SERVICE_TIME = 10
const val SCENARIO_TIME = 40
const val SLEEP_TIME = 10
const val SCENARIO_TIMEOUT = SCENARIO_TIME + SLEEP_TIME * 3
const val TEST_TIMEOUT = SCENARIO_TIMEOUT * 3
const val TEST_OVERHEAD = 5
const val REPEAT_TIMES = 11

@RunWith(PerfTestJUnit4Runner::class)
@PerfTest(
    packageName = "ru.yandex.yandexnavi.inhouse",
    description = "Measure rendering time percentiles", owners = ["scorpy"],
    timeoutSeconds = (TEST_TIMEOUT + SERVICE_TIME + TEST_OVERHEAD) * REPEAT_TIMES, defaultRepeat = REPEAT_TIMES
)
class PerformanceTest {
    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val percentiles = intArrayOf(50, 90, 95)

    @Test
    fun performance() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device.pressHome()
        val context = instrumentation.context
        val scenarioTimeMs = SCENARIO_TIME * 1000
        val uri = Uri.parse("yandexnavi://run_map_perf_test?scroll=$scenarioTimeMs&zoom=$scenarioTimeMs&overview=$scenarioTimeMs")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))

        Thread.sleep(TEST_TIMEOUT * 1000.toLong())
    }
}
