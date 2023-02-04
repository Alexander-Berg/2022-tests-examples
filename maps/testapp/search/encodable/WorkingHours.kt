@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.State
import com.yandex.mapkit.search.WorkingHours

class StateEncodable(it: State) {
    val isOpenNow: Boolean? = it.isOpenNow
    val text: String? = it.text
    val shortText: String? = it.shortText
    val tags: List<String> = it.tags
}

class WorkingHoursEncodable(it: WorkingHours) {
    val text: String = it.text
    val availabilities: List<AvailabilityEncodable> = it.availabilities.map { AvailabilityEncodable(it) }
    val state: StateEncodable? = it.state?.let { StateEncodable(it) }
}
