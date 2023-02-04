@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Response

class ResponseEncodable(it: Response) {
    val metadata: SearchMetadataEncodable = SearchMetadataEncodable(it.metadata)
    val geoObjects: List<GeoObjectEncodable> = it.collection.children
        .mapNotNull{ it.obj }
        .map{ GeoObjectEncodable(it) }
    val isOffline: Boolean = it.isOffline
}
