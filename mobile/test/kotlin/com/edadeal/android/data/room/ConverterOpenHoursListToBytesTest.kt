package com.edadeal.android.data.room

import com.edadeal.android.model.entity.Shop
import com.edadeal.android.model.entity.Weekday
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ConverterOpenHoursListToBytesTest(private val original: List<Shop.OpenHours>) {

    companion object {
        private val interval0 = Shop.OpenHours.Interval(from = "08:00", to = "20:00")
        private val interval1 = Shop.OpenHours.Interval(from = "", to = "20:00")
        private val interval2 = Shop.OpenHours.Interval(from = "08:00", to = "")
        private val interval3 = Shop.OpenHours.Interval(from = "", to = "")

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<List<Shop.OpenHours>> = listOf(
            emptyList(),
            listOf(Shop.OpenHours.EMPTY),
            listOf(Shop.OpenHours(intervals = setOf(interval3), weekdays = emptySet())),
            listOf(Shop.OpenHours(intervals = emptySet(), weekdays = setOf(Weekday.Sunday))),
            listOf(Shop.OpenHours(
                intervals = setOf(interval0, interval3, interval1, interval2),
                weekdays = Weekday.values().toSet()
            ))
        )
    }

    @Test
    fun `assert that conversion does not change data`() {
        assertThat(original, containsInAnyOrder(convert(original)))
    }

    private fun convert(items: List<Shop.OpenHours>): List<TypeSafeMatcher<Shop.OpenHours>> {
        val converter = Converter()
        return converter.bytesToOpenHoursList(converter.openHoursListToBytes(items)).orEmpty()
            .map { makeMatcher(it) { a, b -> a.weekdays == b.weekdays && a.intervals == b.intervals } }
    }
}
