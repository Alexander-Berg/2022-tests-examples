package ru.auto.ara.filter.screen.commercial

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.network.Wheel
import ru.auto.core_ui.util.Consts
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class CommercialCommonSectionTest : CommercialFilterTest() {

    @Test
    fun `default value of wheel within light commercial category is left`() {
        val actualValue = defaultTestedScreen.getValueFieldById<Option>(Filters.WHEEL_FIELD).value
        assertEquals(Wheel.ANY, actualValue.key)
    }

    @Test
    fun `default value of wheel within bus category is left`() {
        val actualValue = screen(Consts.BUS_SUB_CATEGORY_ID).getValueFieldById<Option>(Filters.WHEEL_FIELD).value
        assertEquals(Wheel.ANY, actualValue.key)
    }

    @Test
    fun `default value of wheel within truck category is left`() {
        val actualValue = screen(Consts.TRUCK_SUB_CATEGORY_ID).getValueFieldById<Option>(Filters.WHEEL_FIELD).value
        assertEquals(Wheel.ANY, actualValue.key)
    }

    @Test
    fun `default value of wheel within truck tractor category is left`() {
        val actualValue = screen(Consts.TRUCK_TRACTOR_SUB_CATEGORY_ID).getValueFieldById<Option>(Filters.WHEEL_FIELD).value
        assertEquals(Wheel.ANY, actualValue.key)
    }

    @Test
    fun `color field of bus screen should convert to param as several colors with id = COLOR_ID_FIELD`() {
        val colors = setOf("000000", "ffffff", "ff0000")
        val screen = screen(Consts.BUS_SUB_CATEGORY_ID)
        screen.getValueFieldById<Set<String>>(Filters.COLOR_ID_FIELD).value = colors
        val params = screen.searchParams
        assertMultiSelectParams(params, Filters.COLOR_ID_FIELD, colors)
        assertMultiSelectParams(params, Filters.COLOR_FIELD, emptySet())
    }
}
