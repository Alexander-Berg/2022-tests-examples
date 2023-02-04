@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Availability
import com.yandex.mapkit.search.TimeRange

class TimeRangeEncodable(it: TimeRange) {
    val isTwentyFourHours: Boolean? = it.isTwentyFourHours
    val from: Int? = it.from
    val to: Int? = it.to
}

class AvailabilityEncodable(it: Availability) {
    val days: Int = it.days
    val timeRanges: List<TimeRangeEncodable> = it.timeRanges.map { TimeRangeEncodable(it) }
}
