@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.LocalizedValue

class LocalizedValueEncodable(it: LocalizedValue) {
    val value: Double = it.value
    val text: String = it.text
}

