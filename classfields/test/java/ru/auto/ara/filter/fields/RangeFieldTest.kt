package ru.auto.ara.filter.fields

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.network.PreciseRangeMapper
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class RangeFieldTest {

    @Test
    fun checkEngineFilterParams() {
        val field = RangeField.Builder()
            .withMapper(PreciseRangeMapper())
            .buildVolume() as RangeField
        assertEquals(0.1, field.step)
        assertEquals(0.2, field.min)
        assertEquals(10.0, field.max)
        assertEquals("л", field.dimension)
        assertEquals("Любой", field.emptyValue)
        assertEquals("Объём двигателя", field.title)
    }
}
