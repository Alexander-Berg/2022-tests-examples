package ru.yandex.market.perftests.scenario.catalog

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findNavigateToCatalogButton
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitCatalogRecycler
import ru.yandex.market.perftests.dsl.waitForIdle
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenCatalogTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCatalogTest : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitForIdle()
        waitMediaCarousel()

        val navigateToCatalogButton = findNavigateToCatalogButton()

        waitBeforeMeasure()
        measure("catalog_shown") {
            navigateToCatalogButton.click()
            waitCatalogRecycler()
        }
    }
}