package ru.yandex.market.test.util

import ru.yandex.market.mocks.model.checkout.DateItemMock
import ru.yandex.market.mocks.model.checkout.DeliveryItemMock
import ru.yandex.market.mocks.state.MockDate
import ru.yandex.market.test.util.DateUtils.getMonthNameGenitive
import ru.yandex.market.test.util.DateUtils.getWeekDayNameAccusative
import ru.yandex.market.test.util.DateUtils.getWeekDayNameNominative
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR
import java.util.Date

object DeliveryDateUtils {

    private val calendar = Calendar.getInstance()

    fun getDeliveryDateText(delivery: DeliveryItemMock, weekDayCase: WeekDayCase): String {
        return getDeliveryDateText(delivery.beginDate, delivery.endDate, weekDayCase)
    }

    fun getDeliveryDateText(beginDate: DateItemMock, endDate: DateItemMock, weekDayCase: WeekDayCase): String {

        return when {
            beginDate == endDate -> {
                calendar.set(beginDate.year, beginDate.month - 1, beginDate.day, 0, 0, 0)

                val weekDayNumber = calendar.get(Calendar.DAY_OF_WEEK)

                val weekDay = when (weekDayCase) {
                    WeekDayCase.NOMINATIVE -> getWeekDayNameNominative(weekDayNumber)
                    WeekDayCase.ACCUSATIVE -> getWeekDayNameAccusative(weekDayNumber)
                }
                val month = getMonthNameGenitive(beginDate.month - 1)

                String.format(
                    "%s, %d\u00A0%s",
                    weekDay,
                    beginDate.day,
                    month
                )
            }
            beginDate.month == endDate.month -> {
                val month = getMonthNameGenitive(beginDate.month - 1)

                String.format(
                    "%d — %d\u00A0%s",
                    beginDate.day,
                    endDate.day,
                    month
                )
            }
            else -> {
                val beginMonth = getMonthNameGenitive(beginDate.month - 1)
                val endMonth = getMonthNameGenitive(endDate.month - 1)

                String.format(
                    "%d\u00A0%s — %d\u00A0%s",
                    beginDate.day,
                    beginMonth,
                    endDate.day,
                    endMonth
                )
            }
        }
    }

}

fun Date.toMockDate(): MockDate {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return MockDate(
        year = calendar.get(YEAR),
        month = calendar.get(MONTH) + 1,
        day = calendar.get(DAY_OF_MONTH)
    )
}

fun MockDate.toJavaDate(): Date {
    val mock = this
    val calendar = Calendar.getInstance().apply {
        set(mock.year, mock.month - 1, mock.day)
    }
    return calendar.time
}
