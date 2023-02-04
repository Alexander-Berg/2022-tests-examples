@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Phone
import com.yandex.mapkit.search.PhoneType

class PhoneEncodable(it: Phone) {
    val type: PhoneType = it.type
    val formattedNumber: String = it.formattedNumber
    val info: String? = it.info
    val country: String? = it.country
    val prefix: String? = it.prefix
    val ext: String? = it.ext
    val number: String? = it.number
}
