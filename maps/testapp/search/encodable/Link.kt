@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.SearchLink

class LinkEncodable(it: SearchLink) {
    val aref: String? = it.aref
    val link: AttributionEncodable.LinkEncodable = AttributionEncodable.LinkEncodable(it.link)
    val tag: String? = it.tag
}

