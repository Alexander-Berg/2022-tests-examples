package ru.auto.core_ui.util

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.util.Clock
import ru.auto.data.ISO_DATE_TIME_FORMAT
import ru.auto.data.util.getRuLocale
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureParametrizedRunner::class)
class DateExtTest(val testCase: TestCase) {

    @Before
    fun setUp() {
        Clock.impl = object : Clock {
            override fun nowMillis() = "2021-01-29T16:11:19Z".parseDate().time
        }
    }

    @Test
    fun `should format time correctly`() {
        assertEquals(testCase.date.formatTimeYesterdayDay(), testCase.expectedFormat)
    }


    companion object {
        private val dateFormat = SimpleDateFormat(ISO_DATE_TIME_FORMAT, getRuLocale())

        fun String.parseDate() = dateFormat.parse(this)!!

        data class TestCase(val date: Date, val expectedFormat: String) {
            override fun toString(): String = "should return $expectedFormat when formatting $date"
        }

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun cases() = listOf(
            TestCase("2021-01-29T12:11:19Z".parseDate(),"12:11"),
            TestCase("2021-01-28T12:11:19Z".parseDate(),"Вчера 12:11"),
            TestCase("2021-01-27T12:11:19Z".parseDate(),"27 января"),
            TestCase("2020-06-27T12:11:19Z".parseDate(),"27 июня")
        )
    }
}
