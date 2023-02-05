package com.yandex.launcher.alice.vanga

import androidx.collection.ArrayMap
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.yandex.launcher.common.loaders.http2.ResponseInfo
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.vanga.LauncherVangaItem
import com.yandex.launcher.vanga.VangaCountersResponse
import com.yandex.launcher.vanga.VangaLoadCallbacks
import org.junit.Test
import java.io.ByteArrayInputStream


private const val FULL_FILLED_RESPONSE: String =
    """{"lifetime_seconds":123,"stats":[{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

private const val FULL_FILLED_RESPONSE_WITH_2_ITEMS: String =
    """{"lifetime_seconds":123,"stats":[{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1},"weekly":{"5":2}},{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

private const val EMPTY_RESPONSE: String = """{}"""
private const val RESPONSE_WITH_ABSENT_CLASS_NAME_ITEM: String =
    """{"lifetime_seconds":123,"stats":[{"package_name":"com.yandex.test","personal":3,"hourly":{"20":1},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

private const val RESPONSE_WITH_ABSENT_PACKAGE_NAME_ITEM: String =
    """{"lifetime_seconds":123,"stats":[{"class_name":"com.yandex.MainActivity","personal":100500,"hourly":{"12":12},"weekly":{"7":7}},{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

private const val RESPONSE_WITH_ABSENT_HOURS_IN_ONE_ITEM: String =
    """{"lifetime_seconds":123,"stats":[{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"weekly":{"7":7}},{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

private const val RESPONSE_WITH_ABSENT_DAYS_IN_ONE_ITEM: String =
    """{"lifetime_seconds":123,"stats":[{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"12":12}},{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

private const val RESPONSE_WITH_BROKEN_HOUR_KEY: String =
    """{"lifetime_seconds":123,"stats":[{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1,"abc": 2},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

private const val RESPONSE_WITH_BROKEN_HOUR_VALUE: String =
    """{"lifetime_seconds":123,"stats":[{"package_name":"com.yandex.test","class_name":"com.yandex.MainActivity","personal":3,"hourly":{"20":1,"21": "abc"},"weekly":{"5":2},"total":4,"total_hourly":{"19":4},"total_weekly":{"3":4}}]}"""

class ParseVangaResponseTest: BaseRobolectricTest() {

    private val deafultItem: LauncherVangaItem
    private val deafultItemWithoutClassName: LauncherVangaItem
    private val vangaLoadCallbacks = VangaLoadCallbacksForTest()

    init {
        val personalHours = ArrayMap<Int, Int>()
        personalHours[20] = 1
        val personalDays = ArrayMap<Int, Int>()
        // the key of default item is 5 + 1 = 6, because Calendar.getInstance() numerates days starting from 1(not 0)
        personalDays[6] = 2
        val totalHours = ArrayMap<Int, Int>()
        totalHours[19] = 4
        val totalDays = ArrayMap<Int, Int>()
        totalDays[4] = 4
        deafultItem = LauncherVangaItem(
            "com.yandex.test",
            "com.yandex.MainActivity",
            3,
            personalHours,
            personalDays,
            4,
            totalHours,
            totalDays
        )
        deafultItemWithoutClassName =
            LauncherVangaItem("com.yandex.test", null, 3, personalHours, personalDays, 4, totalHours, totalDays)
    }

    @Test
    fun `parse response, lifetime_seconds parsed`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(FULL_FILLED_RESPONSE.toByteArray()), null)

        assertThat(response.lifetimeSeconds, equalTo(123L))
    }

    @Test
    fun `parse response, array with size 1 parsed`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(FULL_FILLED_RESPONSE.toByteArray()), null)

        assertThat(response.items.size, equalTo(1))
    }

    @Test
    fun `parse response, array with size 2 parsed`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(FULL_FILLED_RESPONSE_WITH_2_ITEMS.toByteArray()), null)

        assertThat(response.items.size, equalTo(2))
    }

    @Test
    fun `parse response, no lifetime_seconds received, lifetime_seconds is 0`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(EMPTY_RESPONSE.toByteArray()), null)

        assertThat(response.lifetimeSeconds, equalTo(0L))
    }

    @Test
    fun `parse response, no starts, response contains empty list`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(EMPTY_RESPONSE.toByteArray()), null)

        assertThat(response.items.isEmpty(), equalTo(true))
    }

    @Test
    fun `parse response, item has no package name, item skipped`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(RESPONSE_WITH_ABSENT_PACKAGE_NAME_ITEM.toByteArray()), null)

        assertThat(response.items.size, equalTo(1))
        assertThat(response.items[0], equalTo(deafultItem))
    }

    @Test
    fun `parse response, item has no class name, item parsed`() {
        val response =
            vangaLoadCallbacks.readData(ByteArrayInputStream(RESPONSE_WITH_ABSENT_CLASS_NAME_ITEM.toByteArray()), null)

        assertThat(response.items.size, equalTo(1))
        assertThat(response.items[0], equalTo(deafultItemWithoutClassName))
    }

    @Test
    fun `parse response, item has no hours, item skipped`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(RESPONSE_WITH_ABSENT_HOURS_IN_ONE_ITEM.toByteArray()), null)

        assertThat(response.items.size, equalTo(1))
        assertThat(response.items[0], equalTo(deafultItem))
    }

    @Test
    fun `parse response, item has no days, item skipped`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(RESPONSE_WITH_ABSENT_DAYS_IN_ONE_ITEM.toByteArray()), null)

        assertThat(response.items.size, equalTo(1))
        assertThat(response.items[0], equalTo(deafultItem))
    }

    @Test
    fun `parse response, hours has broken hour key, broken hour is skipped`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(RESPONSE_WITH_BROKEN_HOUR_KEY.toByteArray()), null)

        assertThat(response.items.size, equalTo(1))
        assertThat(response.items[0], equalTo(deafultItem))
    }

    @Test
    fun `parse response, hours has broken hour value, broken hour is skipped`() {
        val response = vangaLoadCallbacks.readData(ByteArrayInputStream(RESPONSE_WITH_BROKEN_HOUR_VALUE.toByteArray()), null)

        assertThat(response.items.size, equalTo(1))
        assertThat(response.items[0], equalTo(deafultItem))
    }
}

private class VangaLoadCallbacksForTest: VangaLoadCallbacks() {
    override fun onLoadError(responseInfo: ResponseInfo) { /* not used */ }
    override fun onDataLoaded(data: VangaCountersResponse?, responseInfo: ResponseInfo) { /* not used */ }
}