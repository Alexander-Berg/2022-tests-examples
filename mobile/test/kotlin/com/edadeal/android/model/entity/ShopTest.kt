package com.edadeal.android.model.entity

import com.edadeal.android.model.Time
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ShopTest(private val hours: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun millis() = 0..7 * 24

        private const val time0 = "00:00"
        private const val time3 = "03:00"
        private const val time8 = "08:00"
        private const val time20 = "20:00"
        private const val time24 = "24:00"
        // MO for Monday and so on
        private val MO_SU = Weekday.values().toSet()
        private val MO_FR = MO_SU - Weekday.Sunday - Weekday.Saturday
        private val MO_TH = MO_FR - Weekday.Friday
        private val time0_24 = setOf(Shop.OpenHours.Interval(time0, time24))
        private val time8_24 = setOf(Shop.OpenHours.Interval(time8, time24))
        private val time8_20 = setOf(Shop.OpenHours.Interval(time8, time20))
        private val time0_3_and_8_24 = setOf(
            Shop.OpenHours.Interval(time0, time3), Shop.OpenHours.Interval(time8, time24))
    }

    private fun createShop(weekdays: Set<Weekday>, intervals: Set<Shop.OpenHours.Interval>): Shop {
        return Shop.EMPTY.copy(openHours = listOf(Shop.OpenHours(weekdays, intervals)))
    }

    @Test
    fun `assert OpenHours is built correctly`() {
        val time = Time { Time.hoursToMillis(hours) }
        val now = time.now()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val weekday = Weekday.fromCalendar(now)

        val expected_MO_SU_0_24 = Shop.OpenHoursResult(is247 = true)
        val expected_MO_SU_8_24 = when (hour >= 8) {
            true -> Shop.OpenHoursResult(isOpen = true, tillTime = time24)
            else -> Shop.OpenHoursResult(isOpen = false, tillTime = time8)
        }
        val expected_MO_SU_0_3_8_24 = when (hour < 3 || hour >= 8) {
            true -> Shop.OpenHoursResult(isOpen = true, tillTime = time3)
            else -> Shop.OpenHoursResult(isOpen = false, tillTime = time8)
        }
        val expected_MO_FR_8_24 = when {
            MO_FR.contains(weekday) && hour < 8 -> Shop.OpenHoursResult(isOpen = false, tillTime = time8)
            MO_FR.contains(weekday) && hour in 8..19 -> Shop.OpenHoursResult(isOpen = true, tillTime = time20)
            MO_TH.contains(weekday) && hour >= 20 -> Shop.OpenHoursResult(isOpen = false, tillTime = time8)
            weekday == Weekday.Sunday && hour >= 8 -> Shop.OpenHoursResult(isOpen = false, tillTime = time8)
            else -> Shop.OpenHoursResult(isOpen = false, tillDay = Weekday.Monday)
        }

        assertEquals(expected_MO_SU_0_24, Shop.getOpenHoursResult(createShop(MO_SU, time0_24).openHours, time))
        assertEquals(expected_MO_SU_8_24, Shop.getOpenHoursResult(createShop(MO_SU, time8_24).openHours, time))
        assertEquals(expected_MO_SU_0_3_8_24, Shop.getOpenHoursResult(createShop(MO_SU, time0_3_and_8_24).openHours, time))
        assertEquals(expected_MO_FR_8_24, Shop.getOpenHoursResult(createShop(MO_FR, time8_20).openHours, time))
    }
}
