package ru.yandex.yandexnavi

import com.yandex.perftests.core.IntervalReporter
import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import com.yandex.perftests.runner.PerfTestUtils
import com.yandex.perftests.runner.ProcessReporter
import org.junit.Test
import org.junit.runner.RunWith

const val naviAppPackage = "ru.yandex.yandexnavi.inhouse"

@RunWith(PerfTestJUnit4Runner::class)
@PerfTest(
    packageName = naviAppPackage,
    description = "Measure application startup time and CPU usage", owners = ["scorpy"],
    timeoutSeconds = 600, defaultRepeat = 21
)
class AppLaunchTest {
    private val utils = PerfTestUtils(naviAppPackage)

    @Test
    fun coldStart() {
        val reporter = ProcessReporter(naviAppPackage, utils.device)
        IntervalReporter("coldStart").use {
            utils.startMainActivity()
            utils.device.waitForIdle()
        }
        reporter.report("coldStart")
    }
}
