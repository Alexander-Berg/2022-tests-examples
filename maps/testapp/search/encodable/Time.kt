@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.Time

class TimeEncodable(it: Time) {
    val value: Long = it.value
    val tzOffset: Int = it.tzOffset
    val text: String = it.text
}
