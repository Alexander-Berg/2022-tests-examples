@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Chain

class ChainEncodable(it: Chain) {
    val id: String = it.id
    val name: String = it.name
}
