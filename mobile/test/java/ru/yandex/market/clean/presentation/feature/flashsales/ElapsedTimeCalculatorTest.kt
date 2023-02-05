package ru.yandex.market.clean.presentation.feature.flashsales

import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import ru.yandex.market.feature.timer.ui.ElapsedTimeCalculator
import ru.yandex.market.utils.createDate
import ru.yandex.market.utils.days
import ru.yandex.market.utils.minus
import ru.yandex.market.utils.plus
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.utils.millis

class ElapsedTimeCalculatorTest {

    private val dateTimeProvider = mock<DateTimeProvider> {
        on { currentUnixTimeInMillis } doReturn NOW.time
    }
    private val calculator = ElapsedTimeCalculator(dateTimeProvider)

    @Test
    fun `Uses unix time when calculating elapsed time`() {
        calculator.getMillisecondsUntil(123)
        verify(dateTimeProvider).currentUnixTimeInMillis
    }

    @Test
    fun `Normalize negative value to zero`() {
        assertThat(calculator.getMillisecondsUntil((NOW - 1.days).time)).isEqualTo(0L)
    }

    @Test
    fun `Normalize values over maximum elapsed time to maximum value`() {
        val maximumElapsedMillis = calculator.getMaximumElapsedMilliseconds()
        val endTime = (NOW + maximumElapsedMillis.millis + 1.days).time
        assertThat(calculator.getMillisecondsUntil(endTime)).isEqualTo(maximumElapsedMillis)
    }

    companion object {
        private val NOW = createDate(2020, 4, 7, 15, 44, 0, 0)
    }
}