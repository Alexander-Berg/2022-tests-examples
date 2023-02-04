@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.TransitObjectMetadata

class TransitObjectMetadataEncodable(it: TransitObjectMetadata) {
    val routeId: String = it.routeId
    val types: List<String> = it.types
}
