@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.Money

class MoneyEncodable(it: Money) {
    val value: Double = it.value
    val text: String = it.text
    val currency: String = it.currency
}
