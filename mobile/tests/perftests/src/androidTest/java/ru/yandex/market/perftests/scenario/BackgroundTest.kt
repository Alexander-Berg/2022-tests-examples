package ru.yandex.market.perftests.scenario

import android.os.SystemClock
import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import com.yandex.perftests.runner.ProcessReporter
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.measureCpuTick
import ru.yandex.market.perftests.dsl.measureTime
import ru.yandex.market.perftests.dsl.pressHome
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitForIdle
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import java.util.concurrent.TimeUnit

/**
 * Адаптированый com.yandex.perftests.tests.BackgroundTestBase
 */

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "Background test",
    owners = [Owners.DMTRIY_DOGAEV],
    timeoutSeconds = Constants.LONG_TIMEOUT,
    defaultRepeat = 3,
    skipFirstResult = false
)
@RunWith(PerfTestJUnit4Runner::class)
class BackgroundTest : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitForIdle()
        waitMediaCarousel()
        pressHome()

        measureCpuTick("background:total") {

            val procReporter = ProcessReporter(packageName, device)
            val deadline = SystemClock.elapsedRealtimeNanos() + TimeUnit.SECONDS.toNanos(MEASUREMENT_DURATION_S)

            measureTime("background:initial") {
                TimeUnit.SECONDS.sleep(INITIAL_DELAY_S)
            }

            procReporter.report("background:initial")

            TimeUnit.SECONDS.sleep(MEASUREMENT_PERIOD_S)
            while (SystemClock.elapsedRealtimeNanos() < deadline) {
                measureTime("background") {
                    TimeUnit.SECONDS.sleep(MEASUREMENT_PERIOD_S)
                }
                procReporter.report("background")
            }

            procReporter.report("background")
        }
    }

    companion object {
        private const val MEASUREMENT_DURATION_S = 180L
        private const val INITIAL_DELAY_S = 3L
        private const val MEASUREMENT_PERIOD_S = 5L
    }
}