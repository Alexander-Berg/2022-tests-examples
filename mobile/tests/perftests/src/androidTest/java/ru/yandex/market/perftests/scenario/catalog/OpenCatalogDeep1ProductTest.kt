package ru.yandex.market.perftests.scenario.catalog

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findProductImageInCatalogRecycler
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.openCatalogHomeCategory
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.dsl.waitProductRootRecycler
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenProductInCatalogDeep1Test",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCatalogDeep1ProductTest : MarketBasePerfTest() {

    override fun scenario() {
        openCatalogHomeCategory()

        val productImage = findProductImageInCatalogRecycler()
        waitBeforeMeasure()
        measure("open_product_in_catalog_deep_1") {
            productImage.click()
            waitProductRootRecycler()
            waitMediaCarousel()
        }
    }
}