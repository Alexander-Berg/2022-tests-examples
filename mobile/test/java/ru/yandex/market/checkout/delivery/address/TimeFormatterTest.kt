package ru.yandex.market.checkout.delivery.address

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.common.LocalTime

class TimeFormatterTest {

    private var timeFormatter = TimeFormatter()

    @Test
    fun `Format short works as expected`() {
        val localTime = LocalTime.builder()
            .hours(11)
            .minutes(5)
            .seconds(55)
            .build()
        val formatted = timeFormatter.formatShort(localTime)
        assertThat(formatted).isEqualTo("11:05")
    }

    @Test
    fun `Format short with seconds works as expected`() {
        val localTime = LocalTime.builder()
            .hours(9)
            .minutes(31)
            .seconds(7)
            .build()
        val formatted = timeFormatter.formatShortWithSeconds(localTime)
        assertThat(formatted).isEqualTo("09:31:07")
    }

    @Test
    fun `Format short with day end formats day end as 24-00`() {
        val formatted = timeFormatter.formatShortWithDayEnd(LocalTime.dayEnd())
        assertThat(formatted).isEqualTo("24:00")
    }

    @Test
    fun `Format short with day end formats 23-59 as 24-00`() {
        val formatted = timeFormatter.formatShortWithDayEnd(LocalTime.create(23, 59))
        assertThat(formatted).isEqualTo("24:00")
    }
}
