package ru.yandex.market.perftests.scenario.catalog

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findCatalogListItem
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitProductImage

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenCatalogDeep3Test",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCatalogDeep3Test : OpenCatalogDeep3BaseTest() {

    override fun scenario() {
        super.scenario()

        val catalogListItem = findCatalogListItem()

        waitBeforeMeasure()
        measure("catalog_shown_deep_3") {
            catalogListItem.click()
            waitProductImage()
        }
    }
}