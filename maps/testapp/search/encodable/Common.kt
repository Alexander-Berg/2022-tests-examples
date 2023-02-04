@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.Image
import com.yandex.runtime.KeyValuePair

class KeyValuePairEncodable(it: KeyValuePair) {
    val key: String = it.key
    val value: String = it.value
}

class ImageEncodable(it: Image) {
    val urlTemplate: String = it.urlTemplate
    val tags: List<String> = it.tags
}
