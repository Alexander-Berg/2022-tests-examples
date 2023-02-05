package ru.yandex.market.perftests.scenario.catalog

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findHomeCategory
import ru.yandex.market.perftests.dsl.findNavigateToCatalogButton
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitForIdle
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenCatalogDeep1Test",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCatalogDeep1Test : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitForIdle()

        val navigateToCatalogButton = findNavigateToCatalogButton()
        navigateToCatalogButton.click()

        val homeCategory = findHomeCategory()
        waitBeforeMeasure()
        measure("catalog_shown_deep_1") {
            homeCategory.click()
            waitMediaCarousel()
        }
    }
}