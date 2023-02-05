package ru.yandex.yandexbus.perf.tests

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import com.yandex.perftests.runner.ProcessReporter
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(PerfTestJUnit4Runner::class)
@PerfTest(
    packageName = PACKAGE,
    description = "Measure application startup time",
    owners = ["yauhen"],
    requiredMetrics = ["Activity.Created.Cold", "Activity.Drawn.Cold"]
)
class AppStartupTest : BaseTest(AppStartupTest::class.java) {

    @Test
    fun coldStart() {
        val processReporter = ProcessReporter(packageName(), utils().device)
        start()
        processReporter.finish("cold_start")
    }
}
