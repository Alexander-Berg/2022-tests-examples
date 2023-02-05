package ru.yandex.market.network.mapi.headers

import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import ru.yandex.market.common.datetime.AndroidDateTimeProvider
import java.util.TimeZone

@RunWith(BlockJUnit4ClassRunner::class)
class TimezoneMapiHeadersProviderTest : TestCase() {

    private val timezoneMapiHeadersProvider = TimezoneMapiHeadersProvider(AndroidDateTimeProvider())

    @Test
    fun testMoscowTimezone() {
        checkTimezoneMapping("Europe/Moscow", "+03:00")
    }

    @Test
    fun testYekaterinburgTimezone() {
        checkTimezoneMapping("Asia/Yekaterinburg", "+05:00")
    }

    @Test
    fun testKrasnoyarskTimezone() {
        checkTimezoneMapping("Asia/Krasnoyarsk", "+07:00")
    }

    @Test
    fun testMagadanTimezone() {
        checkTimezoneMapping("Asia/Magadan", "+11:00")
    }

    private fun checkTimezoneMapping(timezoneName: String, expectedOffset: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(timezoneName))
        assertEquals(mapOf(MapiHeaders.TIMEZONE to expectedOffset), timezoneMapiHeadersProvider.getHeaders())
    }


}
