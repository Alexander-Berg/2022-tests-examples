package ru.auto.ara.filter.screen.auto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.consts.Filters
import ru.auto.ara.consts.Filters.STATE_FIELD
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.filter.screen.MultiGeoValue
import ru.auto.ara.filter.viewcontrollers.values.PriceLoanValue
import ru.auto.ara.network.State
import ru.auto.ara.network.Wheel
import ru.auto.ara.util.SerializablePair
import ru.auto.core_ui.util.Consts
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals

/**
 * @author aleien on 12.05.17.
 */
@RunWith(AllureRobolectricRunner::class) class AutoCommonSectionTest : AutoFilterTest() {

    @Test
    fun `geo field should convert to param as geo_id and geo_radius`() {
        val geoItem = SuggestGeoItem("213", "Москва", null, true)
        testedScreen.getValueFieldById<MultiGeoValue>(Filters.GEO_FIELD).value =
                MultiGeoValue.fromGeoItems(listOfNotNull(geoItem))
        val params = testedScreen.searchParams

        assertThat(params).containsOnlyOnce(
                SerializablePair(Consts.FILTER_PARAM_GEO_NEW, "${geoItem.id}%${geoItem.name}"),
                SerializablePair(Consts.FILTER_PARAM_GEO_RADIUS, Consts.DEFAULT_RADIUS_KM.toString())
        )
    }

    @Test
    fun `year field should convert to param as year_form and year_to`() {
        val year = SerializablePair(1890.0, 2017.0)

        testedScreen.getValueFieldById<SerializablePair<Double, Double>>(Filters.YEAR_FIELD).value = year

        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.YEAR_FIELD + "[1]", year.first.toInt().toString()),
                SerializablePair(Filters.YEAR_FIELD + "[2]", year.second.toInt().toString()))

    }

    @Test
    fun `run field should convert to param as run_to and run_from`() {
        val multiplier = 1
        val run = SerializablePair(10.0 * multiplier, 10000.0 * multiplier)

        testedScreen.getValueFieldById<SerializablePair<Double, Double>>(Filters.RUN_FIELD).value = run

        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(
                SerializablePair(Filters.RUN_FIELD + "[1]", (run.first.toInt() / multiplier).toString()),
                SerializablePair(Filters.RUN_FIELD + "[2]", (run.second.toInt() / multiplier).toString()))
    }

    @Test
    fun `price field should convert to param as price_from and price_to`() {
        val price = SerializablePair(10000.0, 10000000.0)
        val value = PriceLoanValue(priceRangeValue = price, loanGroup = null)
        testedScreen.getValueFieldById<PriceLoanValue>(Filters.PRICE_WITH_LOAN_FIELD).value = value

        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.PRICE_FIELD + "[1]", price.first.toInt().toString()),
                SerializablePair(Filters.PRICE_FIELD + "[2]", price.second.toInt().toString()))
    }

    @Test
    fun `state field should convert to param as section_id`() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.NEW

        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(STATE_FIELD, State.NEW))
    }

    @Test
    fun `official seller field isn't converted when section_id is ALL`() {
        testedScreen.getValueFieldById<Boolean>(Filters.OFFICIAL_FIELD).value = true

        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(STATE_FIELD, State.ALL))
                .doesNotContain(SerializablePair(Consts.FILTER_PARAM_DEALER_ORG_TYPE, "1"))
    }

    @Test
    fun `official seller field isn't converted when section_id id USED`() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.USED
        testedScreen.getValueFieldById<Boolean>(Filters.OFFICIAL_FIELD).value = true

        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(STATE_FIELD, State.USED))
                .doesNotContain(SerializablePair(Consts.FILTER_PARAM_DEALER_ORG_TYPE, "1"))
    }

    @Test
    fun `default value of wheel is any`() {
        val actualValue = testedScreen.getValueFieldById<Option>(Filters.WHEEL_FIELD).value
        assertEquals(Wheel.ANY, actualValue.key)
    }
}
