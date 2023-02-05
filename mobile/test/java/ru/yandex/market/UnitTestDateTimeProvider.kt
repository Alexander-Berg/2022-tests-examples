package ru.yandex.market

import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.utils.createDate
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

class UnitTestDateTimeProvider @Inject constructor() : DateTimeProvider {

    var currentDate: Date = createDate(2018, 6, 16)

    override val currentDateTime get() = currentDate

    override val currentUnixTimeInMillis get() = currentDate.time

    override val currentUtcTimeInMillis get() = currentDate.time

    override val currentHour: Int = 13

    override val currentMinute: Int = 0

    override val currentYear: Int = 2020

    override val currentDayOfYear: Int = 100

    override val currentDayOfWeek: Int = 1

    override val currentDayOfMonth: Int = 16

    override val currentTimezone: TimeZone = TimeZone.getDefault()
}
