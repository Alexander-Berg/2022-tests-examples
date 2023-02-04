package ru.auto.ara.filter.screen.auto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.network.Wheel
import ru.auto.ara.util.SerializablePair
import ru.auto.test.runner.AllureRobolectricRunner

/**
 * @author aleien on 12.05.17.
 */
@RunWith(AllureRobolectricRunner::class) class AutoGearSectionTest : AutoFilterTest() {

    @Test
    fun `body type field should convert to param as several body types`() {
        val bodyTypes = setOf("g_hatchback", "g_offroad")
        testedScreen.getValueFieldById<Set<String>>(Filters.BODY_TYPE_FIELD).value = bodyTypes
        val params = testedScreen.searchParams
        assertMultiSelectParams(params, Filters.BODY_TYPE_FIELD, bodyTypes)
    }

    @Test
    fun `color field of auto screen should convert to param as several colors with id = COLOR_FIELD`() {
        val colors = setOf("000000", "ffffff", "ff0000")
        testedScreen.getValueFieldById<Set<String>>(Filters.COLOR_FIELD).value = colors
        val params = testedScreen.searchParams
        assertMultiSelectParams(params, Filters.COLOR_FIELD, colors)
        assertMultiSelectParams(params, Filters.COLOR_ID_FIELD, emptySet())
    }

    @Test
    fun `drive field should convert to param as several drives`() {
        val drives = setOf("180", "181")
        testedScreen.getValueFieldById<Set<String>>(Filters.DRIVE_FIELD).value = drives
        val params = testedScreen.searchParams
        assertMultiSelectParams(params, Filters.DRIVE_FIELD, drives)
    }

    @Test
    fun `gearbox field should convert to param as several gearboxes`() {
        val gearboxes = setOf("1", "2", "3")
        testedScreen.getValueFieldById<Set<String>>(Filters.GEARBOX_FIELD).value = gearboxes
        val params = testedScreen.searchParams
        assertMultiSelectParams(params, Filters.GEARBOX_FIELD, gearboxes)
    }

    @Test
    fun `engine_type field should convert to param as several engine types`() {
        val engineTypes = setOf("1256", "1257", "1262")
        testedScreen.getValueFieldById<Set<String>>(Filters.ENGINE_TYPE_FIELD).value = engineTypes
        val params = testedScreen.searchParams
        assertMultiSelectParams(params, Filters.ENGINE_TYPE_FIELD, engineTypes)
    }

    @Test
    fun `wheel field should convert to param as wheel`() {
        testedScreen.getValueFieldById<Option>(Filters.WHEEL_FIELD).value = Option(Wheel.LEFT, "")
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.WHEEL_FIELD, Wheel.LEFT))
    }
}
