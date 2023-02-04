@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.CollectionObjectMetadata

class CollectionObjectMetadataEncodable(it: CollectionObjectMetadata) {
    val collection: CollectionEncodable = CollectionEncodable(it.collection)
}
