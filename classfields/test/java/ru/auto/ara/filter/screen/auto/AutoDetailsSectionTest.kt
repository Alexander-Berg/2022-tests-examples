package ru.auto.ara.filter.screen.auto

import junit.framework.Assert.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.Field
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.data.models.form.state.FieldState
import ru.auto.ara.data.models.form.state.SimpleState
import ru.auto.ara.util.SerializablePair
import ru.auto.test.runner.AllureRobolectricRunner

/**
 * @author aleien on 15.05.17.
 */
@RunWith(AllureRobolectricRunner::class) class AutoDetailsSectionTest : AutoFilterTest() {

    @Test
    fun `pts field should convert to param as pts`() {
        testedScreen.getValueFieldById<Boolean>(Filters.PTS_FIELD).value = true
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.PTS_FIELD, "1"))
    }

    @Test
    fun `exchange field should convert to param as exchange`() {
        testedScreen.getValueFieldById<Boolean>(Filters.EXCHANGE_FIELD).value = true
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.EXCHANGE_FIELD, "4"))
    }

    // Custom = "растаможенный"
    @Test
    fun `custom field should convert to param as custom`() {
        val custom = Option("1", "Растаможен")
        testedScreen.getValueFieldById<Option>(Filters.CUSTOM_FIELD).value = custom
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.CUSTOM_FIELD, custom.key))
    }

    @Test
    fun `purchase date field should convert to param as purchase_field`() {
        val purchase = Option("1", "До 1 года")
        testedScreen.getValueFieldById<Option>(Filters.PURCHASE_DATE_FIELD).value = purchase
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.PURCHASE_DATE_FIELD, purchase.key))
    }

    @Test
    fun `period field should convert to param as period`() {
        val period = Option("7", "За месяц")
        testedScreen.getValueFieldById<Option>(Filters.PERIOD_FIELD).value = period
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.PERIOD_FIELD, period.key))
    }

    @Test
    fun `owners number field should convert to param as owners_number`() {
        val owners = Option("3", "Три")
        testedScreen.getValueFieldById<Option>(Filters.OWNERS_NUMBER_FIELD).value = owners
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.OWNERS_NUMBER_FIELD, owners.key))
    }

    @Test
    fun `photo field should convert to param as photo`() {
        testedScreen.getValueFieldById<Boolean>(Filters.PHOTO_FIELD).value = true
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.PHOTO_FIELD, "1"))
    }

    @Test
    fun `armored_field_is_in_extras`() {
        val checkedValue = "1"
        val armoredState: SimpleState = SimpleState(Field.TYPES.checkbox).apply { fieldName = Filters.ARMORED_STATUS_FIELD}

        assertTrue(testedScreen.fields.filter { it.id == Filters.ARMORED_STATUS_FIELD }.count() == 0)

        testedScreen.getValueFieldById<Map<String, FieldState>>(Filters.EXTRAS_FIELD)
                ?.apply { value = (value?: mapOf<String, SimpleState>()).plus(Filters.ARMORED_STATUS_FIELD to armoredState) }


        val params = testedScreen.searchParams

        assertThat(params).containsOnlyOnce(SerializablePair(Filters.ARMORED_STATUS_FIELD, checkedValue))
    }
}
