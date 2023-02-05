package ru.yandex.market.clean.presentation.feature.purchasebylist.analogs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.purchaseByList.analogs.PurchaseByListAnalogsSubtitleFormatter
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.data.filters.FiltersArrayList
import ru.yandex.market.data.filters.filter.EnumFilter
import ru.yandex.market.data.filters.filter.filterValue.FilterValue

class PurchaseByListAnalogsSubtitleFormatterTest {

    private val resourcesManager = mock<ResourcesManager> {
        on {
            getString(R.string.active_substance)
        } doReturn ACTIVE_SUBSTANCE

        on {
            getString(R.string.drug_with_active_substance)
        } doReturn WITH_ACTIVE_SUBSTANCE

        on {
            getFormattedString(
                R.string.drug_with_fact_active_substance,
                SUBSTANCE
            )
        } doReturn FACT_ACTIVE_SUBSTANCE

        on {
            getFormattedString(
                R.string.active_substance_is,
                SUBSTANCE,
            )
        } doReturn SPECIFIC_ACTIVE_SUBSTANCE
    }

    private val formatter = PurchaseByListAnalogsSubtitleFormatter(
        resourcesManager = resourcesManager
    )

    @Test
    fun `Check correct extended format by correct filters`() {
        val filters = FiltersArrayList.testInstance()
        filters.add(SUBSTANCE_ENUM_FILTER)

        assertThat(formatter.formatExtendedSubtitle(filters)).isEqualTo(FACT_ACTIVE_SUBSTANCE)
    }

    @Test
    fun `Check correct format by correct filters 2`() {
        val filters = FiltersArrayList.testInstance()
        filters.add(SUBSTANCE_ENUM_FILTER)

        assertThat(formatter.formatSubtitle(filters)).isEqualTo(SPECIFIC_ACTIVE_SUBSTANCE)
    }

    @Test
    fun `Check correct format by empty filters`() {
        val filters = FiltersArrayList.testInstance()
        assertThat(formatter.formatExtendedSubtitle(filters)).isEqualTo(WITH_ACTIVE_SUBSTANCE)
    }

    @Test
    fun `Check correct format by null`() {
        assertThat(formatter.formatExtendedSubtitle(null)).isEqualTo(WITH_ACTIVE_SUBSTANCE)
    }

    companion object {
        private const val SUBSTANCE = "Парацетамол"
        private const val ACTIVE_SUBSTANCE = "Действующее вещество"
        private const val SPECIFIC_ACTIVE_SUBSTANCE = "$ACTIVE_SUBSTANCE — $SUBSTANCE"
        private const val WITH_ACTIVE_SUBSTANCE = "Лекарства, у которых такое же действующее вещество"
        private const val FACT_ACTIVE_SUBSTANCE = "Лекарства, у которых такое же действующее вещество — $SUBSTANCE"

        private val SUBSTANCE_FILTER_VALUE = FilterValue().apply {
            name = SUBSTANCE
        }

        private val SUBSTANCE_ENUM_FILTER = EnumFilter().apply {
            name = ACTIVE_SUBSTANCE
            setValue(listOf(SUBSTANCE_FILTER_VALUE))
        }
    }
}
