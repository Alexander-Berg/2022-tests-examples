package ru.yandex.market.perftests.scenario.search

import androidx.annotation.CallSuper
import ru.yandex.market.perftests.dsl.findSearchButton
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.search
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitForIdle
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

abstract class SearchBaseTest : MarketBasePerfTest() {

    @CallSuper
    override fun scenario() {
        startMainActivity()
        waitForIdle()

        val navigateToCatalogButton = findSearchButton()
        navigateToCatalogButton.click()
        waitForIdle()

        search("iphone")
        longWaitForIdle()
    }
}