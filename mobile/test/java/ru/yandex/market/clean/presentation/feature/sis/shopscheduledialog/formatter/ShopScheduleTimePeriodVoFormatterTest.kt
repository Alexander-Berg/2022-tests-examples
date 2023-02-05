package ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.checkout.delivery.address.TimeFormatter
import ru.yandex.market.clean.domain.model.shop.shopWorkScheduleTestInstance
import ru.yandex.market.clean.domain.model.shop.shopWorkScheduleTimeTestInstance
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.vo.ShopScheduleTimePeriodVo
import ru.yandex.market.common.android.ResourcesManager

class ShopScheduleTimePeriodVoFormatterTest {

    private val resourceManager = mock<ResourcesManager> {
        on { getString(R.string.around_the_clock) } doReturn AROUND_THE_CLOCK
    }

    private val timeFormatter = mock<TimeFormatter>()

    private val formatter = ShopScheduleTimePeriodVoFormatter(
        resourcesManager = resourceManager,
        timeFormatter = timeFormatter,
    )

    @Test
    fun `Shop is open until a specific time`() {
        val shopWorkSchedule = shopWorkScheduleTestInstance(
            from = shopWorkScheduleTimeTestInstance(10, 11),
            to = shopWorkScheduleTimeTestInstance(23, 59),
        )
        val period = "${shopWorkSchedule.to?.hour ?: 0}:${shopWorkSchedule.to?.minute ?: 0}"
        val shopScheduleTimePeriod = "$OPEN_TO$period"

        whenever(
            resourceManager.getFormattedString(R.string.open_to, period)
        ) doReturn shopScheduleTimePeriod

        whenever(
            timeFormatter.formatShort(
                shopWorkSchedule.to?.hour ?: 0,
                shopWorkSchedule.to?.minute ?: 0,
            )
        ) doReturn period

        val expectedResult = ShopScheduleTimePeriodVo(
            name = null,
            period = shopScheduleTimePeriod,
        )

        val actualResult = formatter.format(shopWorkSchedule)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `Shop is open whole day (0_00 - 24_00)`() {
        val shopWorkSchedule = shopWorkScheduleTestInstance(
            from = shopWorkScheduleTimeTestInstance(0, 0),
            to = shopWorkScheduleTimeTestInstance(24, 0),
        )

        val expectedResult = ShopScheduleTimePeriodVo(
            name = null,
            period = AROUND_THE_CLOCK,
        )

        val actualResult = formatter.format(shopWorkSchedule)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `Shop is open whole day (0_00 - 23_59)`() {
        val shopWorkSchedule = shopWorkScheduleTestInstance(
            from = shopWorkScheduleTimeTestInstance(0, 0),
            to = shopWorkScheduleTimeTestInstance(23, 59),
        )

        val expectedResult = ShopScheduleTimePeriodVo(
            name = null,
            period = AROUND_THE_CLOCK,
        )

        val actualResult = formatter.format(shopWorkSchedule)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `Shop is open until unknown time (to is null)`() {
        val shopWorkSchedule = shopWorkScheduleTestInstance(
            from = shopWorkScheduleTimeTestInstance(0, 0),
            to = null,
        )

        whenever(resourceManager.getFormattedString(any(), any<String>())) doReturn NOT_USED_STRING

        val actualResult = formatter.format(shopWorkSchedule)
        assertThat(actualResult).isNull()
    }

    @Test
    fun `Shop is open until unknown time (to_hour is null)`() {
        val shopWorkSchedule = shopWorkScheduleTestInstance(
            from = shopWorkScheduleTimeTestInstance(0, 0),
            to = shopWorkScheduleTimeTestInstance(null, 45),
        )

        whenever(resourceManager.getFormattedString(any(), any<String>())) doReturn NOT_USED_STRING

        val actualResult = formatter.format(shopWorkSchedule)
        assertThat(actualResult).isNull()
    }

    @Test
    fun `Shop is open until unknown time (to_minute is null)`() {
        val shopWorkSchedule = shopWorkScheduleTestInstance(
            from = shopWorkScheduleTimeTestInstance(0, 0),
            to = shopWorkScheduleTimeTestInstance(23, null),
        )

        whenever(resourceManager.getFormattedString(any(), any<String>())) doReturn NOT_USED_STRING

        val actualResult = formatter.format(shopWorkSchedule)
        assertThat(actualResult).isNull()
    }

    companion object {
        private const val OPEN_TO = "Открыт до "
        private const val AROUND_THE_CLOCK = "Круглосуточно"
        private const val NOT_USED_STRING = "not used string"
    }
}
