package ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.checkout.delivery.address.TimeFormatter
import ru.yandex.market.checkout.pickup.multiple.WeekDayFormatter
import ru.yandex.market.clean.domain.model.WeekDay
import ru.yandex.market.clean.domain.model.shop.ShopInfo
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.DAILY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.DIALOG_SHOP_SCHEDULE_TITLE
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.FRIDAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.MONDAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SATURDAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_INFO_ALL_SCHEDULES_SAME
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_INFO_DEFAULT_SCHEDULES
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_INFO_EMPTY_SCHEDULES
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_INFO_NULLABLE_SCHEDULES
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_INFO_SCHEDULES_WHOLE_DAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_INFO_SHUFFLED_SCHEDULES
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_INFO_WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_EVERYDAY_FROM_TO
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_EVERYDAY_MIDDAY_PERIOD
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_EVERYDAY_PERIOD
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_VO_ALL_SCHEDULES_SAME
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_VO_DEFAULT_SCHEDULES
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_VO_SCHEDULES_WHOLE_DAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_VO_SHUFFLED_SCHEDULES
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_SCHEDULE_VO_WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_END
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_MIDDAY_END
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_END
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_START
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_MIDDAY_START
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_PERIOD_END
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_PERIOD_START
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SHOP_WORK_SCHEDULE_TIME_START
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.SUNDAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.THURSDAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.TUESDAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.WEDNESDAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.WEEKDAYS
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.WEEKENDS
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleVoFormatterTestEntity.WHOLE_DAY
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.vo.ShopScheduleVo
import ru.yandex.market.common.android.ResourcesManager

@RunWith(Parameterized::class)
class ShopScheduleVoFormatterTest(
    private val input: ShopInfo,
    private val expectedResult: ShopScheduleVo
) {

    private val resourcesManager = mock<ResourcesManager> {

        on { getString(R.string.dialog_shop_schedule_title) } doReturn DIALOG_SHOP_SCHEDULE_TITLE

        on { getString(R.string.daily) } doReturn DAILY

        on { getString(R.string.weekdays) } doReturn WEEKDAYS

        on { getString(R.string.weekends) } doReturn WEEKENDS

        on { getString(R.string.around_the_clock) } doReturn WHOLE_DAY

        on {
            getFormattedString(
                R.string.dialog_shop_schedule_everyday_period,
                SHOP_WORK_SCHEDULE_TIME_PERIOD_START,
                SHOP_WORK_SCHEDULE_TIME_PERIOD_END,
            )
        } doReturn SHOP_SCHEDULE_EVERYDAY_PERIOD

        on {
            getFormattedString(
                R.string.dialog_shop_schedule_everyday_period,
                SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_START,
                SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_END,
            )
        } doReturn SHOP_SCHEDULE_EVERYDAY_MIDDAY_PERIOD

        on {
            getFormattedString(
                R.string.dialog_shop_schedule_everyday_from_to,
                SHOP_WORK_SCHEDULE_TIME_PERIOD_START,
                SHOP_WORK_SCHEDULE_TIME_PERIOD_END,
            )
        } doReturn SHOP_SCHEDULE_EVERYDAY_FROM_TO

    }

    private val timeFormatter = mock<TimeFormatter> {

        on {
            formatShort(
                SHOP_WORK_SCHEDULE_TIME_START.hour ?: 0,
                SHOP_WORK_SCHEDULE_TIME_START.minute ?: 0,
            )
        } doReturn SHOP_WORK_SCHEDULE_TIME_PERIOD_START

        on {
            formatShort(
                SHOP_WORK_SCHEDULE_TIME_END.hour ?: 0,
                SHOP_WORK_SCHEDULE_TIME_END.minute ?: 0,
            )
        } doReturn SHOP_WORK_SCHEDULE_TIME_PERIOD_END

        on {
            formatShort(
                SHOP_WORK_SCHEDULE_TIME_MIDDAY_START.hour ?: 0,
                SHOP_WORK_SCHEDULE_TIME_MIDDAY_START.minute ?: 0,
            )
        } doReturn SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_START

        on {
            formatShort(
                SHOP_WORK_SCHEDULE_TIME_MIDDAY_END.hour ?: 0,
                SHOP_WORK_SCHEDULE_TIME_MIDDAY_END.minute ?: 0,
            )
        } doReturn SHOP_WORK_SCHEDULE_TIME_MIDDAY_PERIOD_END

    }

    private val weekDayFormatter = mock<WeekDayFormatter> {
        on { formatFull(WeekDay.MONDAY) } doReturn MONDAY
        on { formatFull(WeekDay.TUESDAY) } doReturn TUESDAY
        on { formatFull(WeekDay.WEDNESDAY) } doReturn WEDNESDAY
        on { formatFull(WeekDay.THURSDAY) } doReturn THURSDAY
        on { formatFull(WeekDay.FRIDAY) } doReturn FRIDAY
        on { formatFull(WeekDay.SATURDAY) } doReturn SATURDAY
        on { formatFull(WeekDay.SUNDAY) } doReturn SUNDAY
    }

    private val formatter = ShopScheduleVoFormatter(
        resourcesManager = resourcesManager,
        timeFormatter = timeFormatter,
        weekDayFormatter = weekDayFormatter
    )

    @Test
    fun `Check shop schedule formatter`() {
        assertThat(formatter.format(input)).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun parameters() = listOf(

            // 0 - режим работы не указан
            arrayOf(
                SHOP_INFO_EMPTY_SCHEDULES,
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),

            // 1 - режим работы не указан (содержит nulls)
            arrayOf(
                SHOP_INFO_NULLABLE_SCHEDULES,
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),

            // 2 - в любой день одинаковый режим
            arrayOf(
                SHOP_INFO_ALL_SCHEDULES_SAME,
                SHOP_SCHEDULE_VO_ALL_SCHEDULES_SAME
            ),

            // 3 - в будни один режим, в выходные другой.
            arrayOf(
                SHOP_INFO_WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME,
                SHOP_SCHEDULE_VO_WEEKDAYS_AND_WEEKENDS_SCHEDULES_SAME
            ),

            // 4 - ПН,ВТ,СР,ЧТ,ПТ,СБ - один режим, в ВС другой
            arrayOf(
                SHOP_INFO_DEFAULT_SCHEDULES,
                SHOP_SCHEDULE_VO_DEFAULT_SCHEDULES
            ),

            // 5 - 24/7 Ежедневно Круглосуточно
            arrayOf(
                SHOP_INFO_SCHEDULES_WHOLE_DAY,
                SHOP_SCHEDULE_VO_SCHEDULES_WHOLE_DAY
            ),

            // 6 - дни задублированы и неотсортированы, некоторые отсутствуют
            arrayOf(
                SHOP_INFO_SHUFFLED_SCHEDULES,
                SHOP_SCHEDULE_VO_SHUFFLED_SCHEDULES
            ),

            // 7 - workScheduleList.from is null
            arrayOf(
                SHOP_INFO_DEFAULT_SCHEDULES.run {
                    copy(
                        workScheduleList = workScheduleList.map {
                            it?.copy(from = null)
                        },
                    )
                },
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),

            // 8 - workScheduleList.to is null
            arrayOf(
                SHOP_INFO_DEFAULT_SCHEDULES.run {
                    copy(
                        workScheduleList = workScheduleList.map {
                            it?.copy(to = null)
                        },
                    )
                },
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),

            // 9 - workScheduleList.from.hour is null
            arrayOf(
                SHOP_INFO_DEFAULT_SCHEDULES.run {
                    copy(
                        workScheduleList = workScheduleList.map {
                            it?.copy(
                                from = it.from?.copy(hour = null)
                            )
                        },
                    )
                },
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),

            // 10 - workScheduleList.from.minute is null
            arrayOf(
                SHOP_INFO_DEFAULT_SCHEDULES.run {
                    copy(
                        workScheduleList = workScheduleList.map {
                            it?.copy(
                                from = it.from?.copy(minute = null)
                            )
                        },
                    )
                },
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),

            // 11 - workScheduleList.to.hour is null
            arrayOf(
                SHOP_INFO_DEFAULT_SCHEDULES.run {
                    copy(
                        workScheduleList = workScheduleList.map {
                            it?.copy(
                                to = it.to?.copy(hour = null)
                            )
                        },
                    )
                },
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),

            // 12 - workScheduleList.to.minute is null
            arrayOf(
                SHOP_INFO_DEFAULT_SCHEDULES.run {
                    copy(
                        workScheduleList = workScheduleList.map {
                            it?.copy(
                                to = it.to?.copy(minute = null)
                            )
                        },
                    )
                },
                SHOP_SCHEDULE_VO_EMPTY_SCHEDULES
            ),
        )
    }
}
