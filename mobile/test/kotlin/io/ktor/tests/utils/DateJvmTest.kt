/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.utils

import io.ktor.util.date.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class DateJvmTest {
    @Test
    fun testJvmDate() {
        val dateRaw = GMTDate(1346524199000)
        val date = dateRaw.toJvmDate()

        assertEquals(dateRaw.timestamp, date.time)
    }

    @Test
    fun testJvmDateWithZoneOffset() {
        val gmtDate = GMTDate(0, 0, 12, 1, Month.JANUARY, 2019)
        val convertedDate = gmtDate.toJvmDate().toInstant().atZone(ZoneOffset.systemDefault())

        assertEquals(convertedDate.toInstant().toGMTDate(), gmtDate)
    }

    @Test
    fun testJvmDateWithSimpleDateFormat() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        dateFormat.timeZone = TimeZone.getTimeZone(ZoneOffset.systemDefault())
        val date = dateFormat.parse("2019-01-01T12:00:00").toInstant()

        val gmtDate = GMTDate(0, 0, 12, 1, Month.JANUARY, 2019)
        val format = dateFormat.timeZone.getOffset(gmtDate.timestamp).toLong()
        val convertedDate = gmtDate.toJvmDate().toInstant().minusMillis(format)

        assertEquals(date, convertedDate)
    }

    private fun Instant.toGMTDate(): GMTDate =
        GMTDate(TimeUnit.SECONDS.toMillis(atZone(ZoneOffset.UTC).toEpochSecond()))
}
