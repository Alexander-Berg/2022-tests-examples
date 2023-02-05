package ru.yandex.market.activity.main.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.usecase.tabs.model.TabDomain
import ru.yandex.market.clean.presentation.navigation.Tab

@RunWith(Parameterized::class)
class TabNavigatorToTabDomainMapperTest(
    private val inputTab: Tab,
    private val expectedTab: TabDomain
) {

    private val mapper = TabNavigatorToTabDomainMapper()

    @Test
    fun `check correct mapping`() {
        val actualResult = mapper(inputTab)
        assertThat(actualResult).isEqualTo(expectedTab)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {2} parameter {1} should be changed to {2}")
        @JvmStatic
        fun data() = listOf<Array<*>>(
            arrayOf(Tab.MAIN, TabDomain.MAIN),
            arrayOf(Tab.CATALOG, TabDomain.CATALOG),
            arrayOf(Tab.DISCOUNTS, TabDomain.DISCOUNTS),
            arrayOf(Tab.CART, TabDomain.CART),
            arrayOf(Tab.PROFILE, TabDomain.PROFILE),
            arrayOf(Tab.EXPRESS, TabDomain.EXPRESS)
        )
    }
}