@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Discovery2xObjectMetadata

class Discovery2xObjectMetadataEncodable(it: Discovery2xObjectMetadata) {
    val collections: List<CollectionEncodable> = it.collections.map { CollectionEncodable(it) }
}

