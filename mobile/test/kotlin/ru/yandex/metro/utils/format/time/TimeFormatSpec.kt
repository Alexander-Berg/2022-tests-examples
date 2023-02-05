package ru.yandex.metro.utils.format.time

import android.content.res.Resources
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.mockito.ArgumentCaptor
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import ru.yandex.metro.R

class TimeFormatSpec : Spek({
    describe("time interval formatter") {
        val resources = mock<Resources> {
            val quantityCaptor = ArgumentCaptor.forClass(Any::class.java)
            on { getString(eq(R.string.time_interval), quantityCaptor.capture(), quantityCaptor.capture()) } doAnswer {
                val lastIndex = quantityCaptor.allValues.lastIndex
                "${quantityCaptor.allValues[lastIndex - 1]} – ${quantityCaptor.allValues[lastIndex]}"
            }
        }

        context("input < 0") {
            it("should throw exception") {
                val throwingFunction: () -> Unit = { TimeFormat.formatTimeInterval(resources, Duration.millis(-1)) }
                throwingFunction shouldThrow IllegalArgumentException::class
            }
        }

        beforeEachTest {
            // (GMT): Tuesday, 20 March 2018, 21:00:00
            DateTimeUtils.setCurrentMillisProvider { 1521576000_000 }
            // GMT+04:00
            DateTimeZone.setDefault(DateTimeZone.forOffsetHours(4))
        }

        context("correct input") {
            val testCases = mapOf(
                    Duration.standardHours(1) to "00:00 – 01:00",
                    Duration.standardMinutes(10) to "00:00 – 00:10",
                    Duration.standardMinutes(1) to "00:00 – 00:01",
                    Duration.standardSeconds(59) to "00:00 – 00:01",
                    Duration.standardSeconds(60 + 29) to "00:00 – 00:01",
                    Duration.standardSeconds(60 + 30) to "00:00 – 00:02"

            )
            testCases.forEach { (input, expectedResult) ->
                it("should be $expectedResult") {
                    TimeFormat.formatTimeInterval(resources, input) shouldEqual expectedResult
                }
            }
        }


    }
})
