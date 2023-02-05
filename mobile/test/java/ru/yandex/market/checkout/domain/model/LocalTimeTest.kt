package ru.yandex.market.checkout.domain.model

import com.annimon.stream.Stream
import com.annimon.stream.test.hamcrest.StreamMatcher.assertElements
import org.hamcrest.Matchers.contains
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.common.LocalTime

class LocalTimeTest {

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception when hours is out of range`() {
        LocalTime.create(24, 0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception when minutes is out of range`() {
        LocalTime.create(0, 60, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception when seconds is out of range`() {
        LocalTime.create(0, 0, 60)
    }

    @Test
    fun `Compare local times works as expected`() {
        val one = LocalTime.dayEnd()
        val two = LocalTime.create(14, 21)
        val three = LocalTime.create(0, 1)
        val four = LocalTime.create(0, 0, 1)
        val five = LocalTime.midnight()

        Stream.of(one, two, three, four, five)
            .sorted()
            .custom(assertElements(contains(five, four, three, two, one)))
    }

    @Test
    fun `isDayEnd() returns true for day end`() {
        assertThat(LocalTime.dayEnd().isDayEnd).isTrue
    }

    @Test
    fun `isDayEnd() returns false for other time`() {
        assertThat(LocalTime.create(12, 0).isDayEnd).isFalse
    }

    @Test
    fun `isMidnight() returns true for midnight`() {
        assertThat(LocalTime.midnight().isMidnight).isTrue
    }

    @Test
    fun `isMidnight() returns false for other time`() {
        assertThat(LocalTime.create(12, 0).isMidnight).isFalse
    }

    @Test
    fun `equalOrGreaterThan return true for greater time`() {
        assertThat(LocalTime.dayEnd().equalOrGreaterThan(LocalTime.midnight())).isTrue
    }

    @Test
    fun `equalOrGreaterThan return true for equal time`() {
        assertThat(LocalTime.midnight().equalOrGreaterThan(LocalTime.midnight())).isTrue
    }

    @Test
    fun `equalOrGreaterThan return false for less time`() {
        assertThat(LocalTime.midnight().equalOrGreaterThan(LocalTime.dayEnd())).isFalse
    }
}
