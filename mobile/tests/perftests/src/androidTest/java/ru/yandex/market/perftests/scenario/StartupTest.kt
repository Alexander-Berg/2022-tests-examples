package ru.yandex.market.perftests.scenario

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitSearchInput

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "Startup test",
    owners = [Owners.DMTRIY_DOGAEV]
)
@RunWith(PerfTestJUnit4Runner::class)
class StartupTest : MarketBasePerfTest() {

    override fun scenario() {
        measure("startup") {
            startMainActivity()
            waitSearchInput()
        }
    }
}