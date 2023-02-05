package ru.yandex.market.perftests.scenario.catalog

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findCatalogItem
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.openCatalogHomeCategory
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitCatalogItem
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenCatalogDeep2Test",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCatalogDeep2Test : MarketBasePerfTest() {

    override fun scenario() {
        openCatalogHomeCategory()

        val catalogItem = findCatalogItem()

        waitBeforeMeasure()
        measure("catalog_shown_deep_2") {
            catalogItem.click()
            waitCatalogItem()
        }
    }
}