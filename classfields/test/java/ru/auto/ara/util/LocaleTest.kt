package ru.auto.ara.util

import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.RobolectricTest
import ru.auto.data.util.toEngLowerCase
import ru.auto.test.runner.AllureRobolectricRunner
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureRobolectricRunner::class) class LocaleTest: RobolectricTest() {

    @Test
    fun `method toLowerCase should transform string using specified locale`() {
        assertEquals(CATEGORY.toLowerCase(Locale("tr")), "traıler")
    }

    @Test
    fun `method toEngLowerCase should transform string using eng locale`() {
        assertEquals(CATEGORY.toEngLowerCase(), "trailer")
    }

    companion object {
        private const val CATEGORY = "TRAILER"
    }
}
