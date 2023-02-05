package ru.yandex.market.perftests.scenario.catalog

import androidx.annotation.CallSuper
import ru.yandex.market.perftests.dsl.findCatalogItem
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.openCatalogHomeCategory
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

abstract class OpenCatalogDeep3BaseTest : MarketBasePerfTest() {

    @CallSuper
    override fun scenario() {
        openCatalogHomeCategory()

        val catalogItem = findCatalogItem()
        catalogItem.click()
        longWaitForIdle()
    }
}