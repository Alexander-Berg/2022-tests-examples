@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.EncyclopediaObjectMetadata

class EncyclopediaObjectMetadataEncodable(it: EncyclopediaObjectMetadata) {
    val title: String? = it.title
    val description: String? = it.description
    val attribution: AttributionEncodable? = it.attribution?.let { AttributionEncodable(it) }
}
