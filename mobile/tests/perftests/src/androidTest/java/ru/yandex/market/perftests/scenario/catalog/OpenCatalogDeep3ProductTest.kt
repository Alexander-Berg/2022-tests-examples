package ru.yandex.market.perftests.scenario.catalog

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findCatalogListItem
import ru.yandex.market.perftests.dsl.findProductImageInSearchRecycler
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.dsl.waitProductRootRecycler

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenCatalogDeep3ProductTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCatalogDeep3ProductTest : OpenCatalogDeep3BaseTest() {

    override fun scenario() {
        super.scenario()

        val catalogListItem = findCatalogListItem()

        catalogListItem.click()
        longWaitForIdle()
        val productImage = findProductImageInSearchRecycler()
        waitBeforeMeasure()
        measure("catalog_shown_deep_3_product") {
            productImage.click()
            waitProductRootRecycler()
            waitMediaCarousel()
        }
    }
}